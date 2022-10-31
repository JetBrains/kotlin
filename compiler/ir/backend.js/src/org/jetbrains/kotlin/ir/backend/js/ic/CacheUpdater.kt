/* * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.ic

import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.ir.backend.js.*
import org.jetbrains.kotlin.ir.backend.js.codegen.JsGenerationGranularity
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.JsIrLinker
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.JsIrProgramFragment
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.konan.properties.propertyList
import org.jetbrains.kotlin.library.KLIB_PROPERTY_DEPENDS
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.name.FqName
import java.io.File
import java.util.EnumSet

fun interface JsIrCompilerICInterface {
    fun compile(
        allModules: Collection<IrModuleFragment>,
        dirtyFiles: Collection<IrFile>,
        mainArguments: List<String>?
    ): List<() -> JsIrProgramFragment>
}

fun interface JsIrCompilerICInterfaceFactory {
    fun createCompilerForIC(
        mainModule: IrModuleFragment,
        configuration: CompilerConfiguration
    ): JsIrCompilerICInterface
}

enum class DirtyFileState(val str: String) {
    ADDED_FILE("added file"),
    MODIFIED_CONFIG("modified config"),
    MODIFIED_IR("modified ir"),
    UPDATED_EXPORTS("updated exports"),
    UPDATED_IMPORTS("updated imports"),
    REMOVED_INVERSE_DEPENDS("removed inverse depends"),
    REMOVED_DIRECT_DEPENDS("removed direct depends"),
    REMOVED_FILE("removed file")
}

class CacheUpdater(
    mainModule: String,
    allModules: Collection<String>,
    icCachePaths: Collection<String>,
    private val compilerConfiguration: CompilerConfiguration,
    private val irFactory: () -> IrFactory,
    private val mainArguments: List<String>?,
    private val compilerInterfaceFactory: JsIrCompilerICInterfaceFactory
) {
    private val stopwatch = StopwatchIC().apply { startNext("Loading klibs") }

    private val libraries = loadLibraries(allModules)
    private val dependencyGraph = buildDependenciesGraph(libraries)
    private val configHash = compilerConfiguration.configHashForIC()

    private val cacheMap = libraries.values.zip(icCachePaths).toMap()

    private val mainLibraryFile = KotlinLibraryFile(File(mainModule).canonicalPath)
    private val mainLibrary = libraries[mainLibraryFile] ?: notFoundIcError("main library", mainLibraryFile)

    private val dirtyFileStats = KotlinSourceFileMutableMap<EnumSet<DirtyFileState>>()

    init {
        stopwatch.stop()
    }

    fun getDirtyFileStats(): KotlinSourceFileMap<EnumSet<DirtyFileState>> = dirtyFileStats

    fun getStopwatchLaps() = stopwatch.laps

    private fun MutableMap<KotlinSourceFile, EnumSet<DirtyFileState>>.addDirtFileStat(srcFile: KotlinSourceFile, state: DirtyFileState) {
        when (val stats = this[srcFile]) {
            null -> this[srcFile] = EnumSet.of(state)
            else -> stats.add(state)
        }
    }

    private fun loadLibraries(allModules: Collection<String>): Map<KotlinLibraryFile, KotlinLibrary> {
        val repositories = compilerConfiguration[JSConfigurationKeys.REPOSITORIES] ?: emptyList()
        val allResolvedDependencies = jsResolveLibraries(allModules, repositories, compilerConfiguration.resolverLogger)
        return allResolvedDependencies.getFullList().associateBy { KotlinLibraryFile(it) }
    }

    private fun buildDependenciesGraph(libraries: Map<KotlinLibraryFile, KotlinLibrary>): Map<KotlinLibrary, List<KotlinLibrary>> {
        val nameToKotlinLibrary = libraries.values.associateBy { it.moduleName }
        return libraries.values.associateWith {
            it.manifestProperties.propertyList(KLIB_PROPERTY_DEPENDS, escapeInQuotes = true).map { depName ->
                nameToKotlinLibrary[depName] ?: notFoundIcError("library $depName")
            }
        }
    }

    private fun <T> mapLibrariesInOrder(f: (KotlinLibraryFile) -> T): List<T> {
        val visited = HashSet<KotlinLibrary>(libraries.size)
        val result = ArrayList<T>(libraries.size)
        fun visitLibrary(lib: KotlinLibrary) {
            if (visited.add(lib)) {
                dependencyGraph[lib]?.forEach(::visitLibrary)
                result += f(KotlinLibraryFile(lib))
            }
        }

        visitLibrary(mainLibrary)
        return result
    }

    private class DirtyFileExports(
        override val inverseDependencies: KotlinSourceFileMutableMap<Set<IdSignature>> = KotlinSourceFileMutableMap()
    ) : KotlinSourceFileExports() {
        override fun getExportedSignatures(): Set<IdSignature> = allExportedSignatures

        val allExportedSignatures = hashSetOf<IdSignature>()
    }

    private class DirtyFileMetadata(
        val maybeImportedSignatures: Collection<IdSignature>,

        val oldDirectDependencies: KotlinSourceFileMap<*>,

        override val inverseDependencies: KotlinSourceFileMutableMap<MutableSet<IdSignature>> = KotlinSourceFileMutableMap(),
        override val directDependencies: KotlinSourceFileMutableMap<MutableMap<IdSignature, ICHash>> = KotlinSourceFileMutableMap(),
    ) : KotlinSourceFileMetadata() {
        fun addInverseDependency(lib: KotlinLibraryFile, src: KotlinSourceFile, signature: IdSignature) =
            when (val signatures = inverseDependencies[lib, src]) {
                null -> inverseDependencies[lib, src] = hashSetOf(signature)
                else -> signatures += signature
            }

        fun addDirectDependency(lib: KotlinLibraryFile, src: KotlinSourceFile, signature: IdSignature, hash: ICHash) =
            when (val signatures = directDependencies[lib, src]) {
                null -> directDependencies[lib, src] = hashMapOf(signature to hash)
                else -> signatures[signature] = hash
            }
    }

    private class UpdatedDependenciesMetadata(oldMetadata: KotlinSourceFileMetadata) : KotlinSourceFileMetadata() {
        private val oldInverseDependencies = oldMetadata.inverseDependencies
        private val newExportedSignatures: Set<IdSignature> by lazy { inverseDependencies.flatSignatures() }

        var importedSignaturesModified = false

        override val inverseDependencies = oldMetadata.inverseDependencies.toMutable()
        override val directDependencies = oldMetadata.directDependencies.toMutable()

        override fun getExportedSignatures(): Set<IdSignature> = newExportedSignatures

        fun isExportedSignaturesUpdated() = newExportedSignatures != oldInverseDependencies.flatSignatures()
    }

    private fun KotlinSourceFileMutableMap<UpdatedDependenciesMetadata>.addNewMetadata(
        libFile: KotlinLibraryFile, srcFile: KotlinSourceFile, oldMetadata: KotlinSourceFileMetadata
    ) = this[libFile, srcFile] ?: UpdatedDependenciesMetadata(oldMetadata).also {
        this[libFile, srcFile] = it
    }

    private inner class CacheUpdaterInternal {
        val signatureHashCalculator = IdSignatureHashCalculator()

        private val incrementalCaches = libraries.entries.associate { (libFile, lib) ->
            val cachePath = cacheMap[lib] ?: notFoundIcError("cache path", KotlinLibraryFile(lib))
            libFile to IncrementalCache(lib, cachePath)
        }

        private fun getLibIncrementalCache(libFile: KotlinLibraryFile) =
            incrementalCaches[libFile] ?: notFoundIcError("incremental cache", libFile)

        private fun addFilesWithRemovedDependencies(
            modifiedFiles: KotlinSourceFileMap<KotlinSourceFileMetadata>,
            removedFiles: KotlinSourceFileMap<KotlinSourceFileMetadata>
        ): KotlinSourceFileMap<KotlinSourceFileMetadata> {
            val extraModifiedLibFiles = KotlinSourceFileMutableMap<KotlinSourceFileMetadata>()

            fun addDependenciesToExtraModifiedFiles(dependencies: KotlinSourceFileMap<*>, dirtyState: DirtyFileState) {
                for ((dependentLib, dependentFiles) in dependencies) {
                    val dependentCache = incrementalCaches[dependentLib] ?: continue
                    val alreadyModifiedFiles = modifiedFiles[dependentLib] ?: emptyMap()
                    val alreadyRemovedFiles = removedFiles[dependentLib] ?: emptyMap()
                    val extraModifiedFiles by lazy(LazyThreadSafetyMode.NONE) { extraModifiedLibFiles.getOrPutFiles(dependentLib) }
                    val fileStats by lazy(LazyThreadSafetyMode.NONE) { dirtyFileStats.getOrPutFiles(dependentLib) }
                    for (dependentFile in dependentFiles.keys) {
                        when (dependentFile) {
                            in alreadyModifiedFiles -> continue
                            in alreadyRemovedFiles -> continue
                            in extraModifiedFiles -> continue
                            else -> {
                                val dependentMetadata = dependentCache.fetchSourceFileFullMetadata(dependentFile)
                                extraModifiedFiles[dependentFile] = dependentMetadata
                                fileStats.addDirtFileStat(dependentFile, dirtyState)
                            }
                        }
                    }
                }
            }

            removedFiles.forEachFile { _, _, removedFileMetadata ->
                addDependenciesToExtraModifiedFiles(removedFileMetadata.directDependencies, DirtyFileState.REMOVED_INVERSE_DEPENDS)
                addDependenciesToExtraModifiedFiles(removedFileMetadata.inverseDependencies, DirtyFileState.REMOVED_DIRECT_DEPENDS)
            }

            if (extraModifiedLibFiles.isNotEmpty()) {
                extraModifiedLibFiles.copyFilesFrom(modifiedFiles)
                return extraModifiedLibFiles
            }
            return modifiedFiles
        }

        fun loadModifiedFiles(): KotlinSourceFileMap<KotlinSourceFileMetadata> {
            val removedFilesMetadata = hashMapOf<KotlinLibraryFile, Map<KotlinSourceFile, KotlinSourceFileMetadata>>()

            val modifiedFiles = KotlinSourceFileMap(incrementalCaches.entries.associate { (lib, cache) ->
                val (dirtyFiles, removedFiles, newFiles, modifiedConfigFiles) = cache.collectModifiedFiles(configHash)

                val fileStats by lazy(LazyThreadSafetyMode.NONE) { dirtyFileStats.getOrPutFiles(lib) }
                newFiles.forEach { fileStats.addDirtFileStat(it, DirtyFileState.ADDED_FILE) }
                modifiedConfigFiles.forEach { fileStats.addDirtFileStat(it, DirtyFileState.MODIFIED_CONFIG) }
                removedFiles.forEach { fileStats.addDirtFileStat(it.key, DirtyFileState.REMOVED_FILE) }
                dirtyFiles.forEach {
                    if (it.key !in newFiles && it.key !in modifiedConfigFiles) {
                        fileStats.addDirtFileStat(it.key, DirtyFileState.MODIFIED_IR)
                    }
                }

                if (removedFiles.isNotEmpty()) {
                    removedFilesMetadata[lib] = removedFiles
                }

                lib to dirtyFiles
            })

            return addFilesWithRemovedDependencies(modifiedFiles, KotlinSourceFileMap(removedFilesMetadata))
        }

        fun collectExportedSymbolsForDirtyFiles(
            dirtyFiles: KotlinSourceFileMap<KotlinSourceFileMetadata>
        ): KotlinSourceFileMutableMap<KotlinSourceFileExports> {
            val exportedSymbols = KotlinSourceFileMutableMap<KotlinSourceFileExports>()

            for ((libFile, srcFiles) in dirtyFiles) {
                val exportedSymbolFiles = HashMap<KotlinSourceFile, KotlinSourceFileExports>(srcFiles.size)
                for ((srcFile, srcFileMetadata) in srcFiles) {
                    val loadingFileExports = DirtyFileExports()
                    for ((dependentLib, dependentFiles) in srcFileMetadata.inverseDependencies) {
                        val dependentCache = incrementalCaches[dependentLib] ?: continue
                        val dirtyLibFiles = dirtyFiles[dependentLib] ?: emptyMap()
                        for (dependentFile in dependentFiles.keys) {
                            if (dependentFile !in dirtyLibFiles) {
                                val dependentSrcFileMetadata = dependentCache.fetchSourceFileFullMetadata(dependentFile)
                                dependentSrcFileMetadata.directDependencies[libFile, srcFile]?.let {
                                    loadingFileExports.inverseDependencies[dependentLib, dependentFile] = it.keys
                                    loadingFileExports.allExportedSignatures += it.keys
                                }
                            }
                        }
                    }
                    exportedSymbolFiles[srcFile] = loadingFileExports
                }
                if (exportedSymbolFiles.isNotEmpty()) {
                    exportedSymbols[libFile] = exportedSymbolFiles
                }
            }
            return exportedSymbols
        }

        private fun KotlinSourceFileMutableMap<DirtyFileMetadata>.getExportedSignaturesAndAddMetadata(
            jsIrLinker: JsIrLinker,
            irModule: IrModuleFragment,
            libFile: KotlinLibraryFile,
            dirtySrcFiles: Set<KotlinSourceFile>
        ): Map<IdSignature, IdSignatureSource> {
            val idSignatureToFile = hashMapOf<IdSignature, IdSignatureSource>()
            val moduleDeserializer = jsIrLinker.moduleDeserializer(irModule.descriptor)
            val incrementalCache = getLibIncrementalCache(libFile)
            for (fileDeserializer in moduleDeserializer.fileDeserializers()) {
                val reachableSignatures = fileDeserializer.symbolDeserializer.signatureDeserializer.signatureToIndexMapping()
                val maybeImportedSignatures = HashSet(reachableSignatures.keys)
                val implementedSymbols = collectImplementedSymbol(fileDeserializer.symbolDeserializer.deserializedSymbols)
                for ((signature, symbol) in implementedSymbols) {
                    var symbolCanBeExported = maybeImportedSignatures.remove(signature)
                    resolveFakeOverrideFunction(symbol)?.let { resolvedSignature ->
                        if (resolvedSignature !in implementedSymbols) {
                            maybeImportedSignatures.add(resolvedSignature)
                        }
                        symbolCanBeExported = true
                    }
                    if (symbolCanBeExported) {
                        idSignatureToFile[signature] = IdSignatureSource(libFile, KotlinSourceFile(fileDeserializer.file), symbol)
                    }
                }

                val libSrcFile = KotlinSourceFile(fileDeserializer.file)
                if (libSrcFile in dirtySrcFiles) {
                    val metadata = incrementalCache.fetchSourceFileFullMetadata(libSrcFile)
                    this[libFile, libSrcFile] = DirtyFileMetadata(maybeImportedSignatures, metadata.directDependencies)
                }
            }
            return idSignatureToFile
        }

        private fun DirtyFileMetadata.setAllDependencies(
            idSignatureToFile: Map<IdSignature, IdSignatureSource>,
            updatedMetadata: KotlinSourceFileMap<DirtyFileMetadata>,
            libFile: KotlinLibraryFile,
            srcFile: KotlinSourceFile
        ) {
            val allImportedSignatures = addParentSignatures(maybeImportedSignatures, idSignatureToFile, libFile, srcFile)
            for (importedSignature in allImportedSignatures) {
                val (dependencyLib, dependencyFile) = idSignatureToFile[importedSignature] ?: continue
                signatureHashCalculator[importedSignature]?.also { signatureHash ->
                    addDirectDependency(dependencyLib, dependencyFile, importedSignature, signatureHash)
                } ?: notFoundIcError("signature $importedSignature hash", dependencyLib, dependencyFile)

                updatedMetadata[dependencyLib, dependencyFile]?.also { dependencyMetadata ->
                    dependencyMetadata.addInverseDependency(libFile, srcFile, importedSignature)
                }
            }
        }

        fun rebuildDirtySourceMetadata(
            jsIrLinker: JsIrLinker,
            loadedFragments: Map<KotlinLibraryFile, IrModuleFragment>,
            dirtySrcFiles: KotlinSourceFileMap<KotlinSourceFileExports>,
        ): KotlinSourceFileMap<DirtyFileMetadata> {
            val idSignatureToFile = hashMapOf<IdSignature, IdSignatureSource>()
            val updatedMetadata = KotlinSourceFileMutableMap<DirtyFileMetadata>()

            for ((lib, irModule) in loadedFragments) {
                val libDirtySrcFiles = dirtySrcFiles[lib]?.keys ?: emptySet()
                idSignatureToFile += updatedMetadata.getExportedSignaturesAndAddMetadata(jsIrLinker, irModule, lib, libDirtySrcFiles)
            }

            signatureHashCalculator.addAllSignatureSymbols(idSignatureToFile)

            for ((libFile, srcFiles) in updatedMetadata) {
                val libDirtySrcFiles = dirtySrcFiles[libFile] ?: continue
                for ((srcFile, updatedHeader) in srcFiles) {
                    val dirtySrcFile = libDirtySrcFiles[srcFile] ?: continue
                    dirtySrcFile.inverseDependencies.forEachFile { dependentLibFile, dependentSrcFile, signatures ->
                        signatures.forEach { signature ->
                            val signatureSrc = idSignatureToFile[signature]
                            val dependencyLib = signatureSrc?.lib ?: libFile
                            val dependencyFile = signatureSrc?.src ?: srcFile
                            updatedMetadata[dependencyLib, dependencyFile]?.also { dependencyMetadata ->
                                dependencyMetadata.addInverseDependency(dependentLibFile, dependentSrcFile, signature)
                            }
                        }
                    }

                    updatedHeader.setAllDependencies(idSignatureToFile, updatedMetadata, libFile, srcFile)
                }
            }

            val result = KotlinSourceFileMutableMap<DirtyFileMetadata>()

            for ((libFile, sourceFiles) in dirtySrcFiles) {
                val incrementalCache = getLibIncrementalCache(libFile)
                val srcFileUpdatedMetadata = updatedMetadata[libFile] ?: notFoundIcError("metadata", libFile)
                for (srcFile in sourceFiles.keys) {
                    val srcMetadata = srcFileUpdatedMetadata[srcFile] ?: notFoundIcError("metadata", libFile, srcFile)
                    incrementalCache.updateSourceFileMetadata(srcFile, srcMetadata)
                    result[libFile, srcFile] = srcMetadata
                }
            }

            return result
        }

        private fun KotlinSourceFileMutableMap<UpdatedDependenciesMetadata>.addDependenciesWithUpdatedSignatures(
            libFile: KotlinLibraryFile,
            srcFile: KotlinSourceFile,
            srcFileMetadata: DirtyFileMetadata
        ) {
            // go through dependencies and collect dependencies with updated signatures
            for ((dependencyLibFile, dependencySrcFiles) in srcFileMetadata.directDependencies) {
                val dependencyCache = getLibIncrementalCache(dependencyLibFile)
                for ((dependencySrcFile, newSignatures) in dependencySrcFiles) {
                    val dependencySrcMetadata = dependencyCache.fetchSourceFileFullMetadata(dependencySrcFile)
                    val oldSignatures = dependencySrcMetadata.inverseDependencies[libFile, srcFile] ?: emptySet()
                    if (oldSignatures == newSignatures) {
                        continue
                    }
                    val newMetadata = addNewMetadata(dependencyLibFile, dependencySrcFile, dependencySrcMetadata)
                    newMetadata.inverseDependencies[libFile, srcFile] = newSignatures.keys
                }
            }
        }

        private fun KotlinSourceFileMutableMap<UpdatedDependenciesMetadata>.addDependenciesWithRemovedInverseDependencies(
            libFile: KotlinLibraryFile,
            srcFile: KotlinSourceFile,
            srcFileMetadata: DirtyFileMetadata
        ) {
            // go through old dependencies and look for removed dependencies
            for ((oldDependencyLibFile, oldDependencySrcFiles) in srcFileMetadata.oldDirectDependencies) {
                val dependencyCache = incrementalCaches[oldDependencyLibFile] ?: continue
                val newDirectDependencyFiles = srcFileMetadata.directDependencies[oldDependencyLibFile] ?: emptyMap()
                for (oldDependencySrcFile in oldDependencySrcFiles.keys) {
                    if (oldDependencySrcFile in newDirectDependencyFiles) {
                        continue
                    }
                    val dependencySrcMetadata = dependencyCache.fetchSourceFileFullMetadata(oldDependencySrcFile)
                    if (dependencySrcMetadata.inverseDependencies[libFile, srcFile] != null) {
                        val newMetadata = addNewMetadata(oldDependencyLibFile, oldDependencySrcFile, dependencySrcMetadata)
                        newMetadata.inverseDependencies.removeFile(libFile, srcFile)
                    }
                }
            }
        }

        private fun KotlinSourceFileMutableMap<UpdatedDependenciesMetadata>.addDependentsWithUpdatedImports(
            libFile: KotlinLibraryFile,
            srcFile: KotlinSourceFile,
            srcFileMetadata: DirtyFileMetadata
        ) {
            // go through dependent files and check if their imports were modified
            for ((dependentLibFile, dependentSrcFiles) in srcFileMetadata.inverseDependencies) {
                val dependentCache = incrementalCaches[dependentLibFile] ?: continue
                for ((dependentSrcFile, newSignatures) in dependentSrcFiles) {
                    val dependentSrcMetadata = dependentCache.fetchSourceFileFullMetadata(dependentSrcFile)
                    val dependentSignatures = dependentSrcMetadata.directDependencies[libFile, srcFile] ?: emptyMap()
                    when {
                        dependentSrcMetadata is DirtyFileMetadata -> continue
                        this[dependentLibFile, dependentSrcFile]?.importedSignaturesModified == true -> continue
                        dependentSignatures.any { signatureHashCalculator[it.key] != it.value } -> {
                            val newMetadata = addNewMetadata(dependentLibFile, dependentSrcFile, dependentSrcMetadata)
                            newMetadata.importedSignaturesModified = true
                        }

                        dependentSignatures.keys != newSignatures -> {
                            val newMetadata = addNewMetadata(dependentLibFile, dependentSrcFile, dependentSrcMetadata)
                            newMetadata.directDependencies[libFile, srcFile] = newSignatures.associateWithTo(HashMap(newSignatures.size)) {
                                signatureHashCalculator[it] ?: notFoundIcError("signature $it hash", libFile, srcFile)
                            }
                        }
                    }
                }
            }
        }

        fun collectFilesWithModifiedExportsAndImports(
            loadedDirtyFiles: KotlinSourceFileMap<DirtyFileMetadata>
        ): KotlinSourceFileMap<UpdatedDependenciesMetadata> {
            val filesWithModifiedExportsAndImports = KotlinSourceFileMutableMap<UpdatedDependenciesMetadata>()

            loadedDirtyFiles.forEachFile { libFile, srcFile, srcFileMetadata ->
                filesWithModifiedExportsAndImports.addDependenciesWithUpdatedSignatures(libFile, srcFile, srcFileMetadata)
                filesWithModifiedExportsAndImports.addDependenciesWithRemovedInverseDependencies(libFile, srcFile, srcFileMetadata)
                filesWithModifiedExportsAndImports.addDependentsWithUpdatedImports(libFile, srcFile, srcFileMetadata)
            }

            return filesWithModifiedExportsAndImports
        }

        fun collectFilesToRebuildSignatures(
            filesWithModifiedExports: KotlinSourceFileMap<UpdatedDependenciesMetadata>
        ): KotlinSourceFileMap<KotlinSourceFileExports> {
            val libFilesToRebuild = KotlinSourceFileMutableMap<KotlinSourceFileExports>()

            for ((libFile, srcFiles) in filesWithModifiedExports) {
                val filesToRebuild by lazy(LazyThreadSafetyMode.NONE) { libFilesToRebuild.getOrPutFiles(libFile) }
                val fileStats by lazy(LazyThreadSafetyMode.NONE) { dirtyFileStats.getOrPutFiles(libFile) }
                val cache = getLibIncrementalCache(libFile)

                for ((srcFile, srcFileMetadata) in srcFiles) {
                    val isExportedSignatureUpdated = srcFileMetadata.isExportedSignaturesUpdated()
                    if (isExportedSignatureUpdated || srcFileMetadata.importedSignaturesModified) {
                        // if exported signatures or imported inline functions were modified - rebuild
                        filesToRebuild[srcFile] = srcFileMetadata
                        if (isExportedSignatureUpdated) {
                            fileStats.addDirtFileStat(srcFile, DirtyFileState.UPDATED_EXPORTS)
                        }
                        if (srcFileMetadata.importedSignaturesModified) {
                            fileStats.addDirtFileStat(srcFile, DirtyFileState.UPDATED_IMPORTS)
                        }
                    } else {
                        // if signatures and inline functions are the same - just update cache metadata
                        cache.updateSourceFileMetadata(srcFile, srcFileMetadata)
                    }
                }
            }

            return libFilesToRebuild
        }

        fun updateStdlibIntrinsicDependencies(
            linker: JsIrLinker,
            mainModule: IrModuleFragment,
            loadedFragments: Map<KotlinLibraryFile, IrModuleFragment>,
            dirtyFiles: Map<KotlinLibraryFile, Set<KotlinSourceFile>>
        ) {
            val stdlibDescriptor = mainModule.descriptor.builtIns.builtInsModule
            val (stdlibFile, stdlibIr) = loadedFragments.entries.find {
                it.value.descriptor === stdlibDescriptor
            } ?: notFoundIcError("stdlib loaded fragment")

            val stdlibDirtyFiles = dirtyFiles[stdlibFile] ?: return

            signatureHashCalculator.updateInlineFunctionTransitiveHashes(listOf(stdlibIr))

            val updatedMetadata = KotlinSourceFileMutableMap<DirtyFileMetadata>()
            val idSignatureToFile = updatedMetadata.getExportedSignaturesAndAddMetadata(linker, stdlibIr, stdlibFile, stdlibDirtyFiles)

            signatureHashCalculator.addAllSignatureSymbols(idSignatureToFile)

            updatedMetadata.forEachFile { libFile, srcFile, updatedHeader ->
                updatedHeader.setAllDependencies(idSignatureToFile, updatedMetadata, libFile, srcFile)
            }

            val incrementalCache = getLibIncrementalCache(stdlibFile)
            updatedMetadata.forEachFile { libFile, srcFile, updatedHeader ->
                if (libFile != stdlibFile) {
                    icError("unexpected lib while parsing stdlib dependencies", libFile, srcFile)
                }

                val cachedHeader = incrementalCache.fetchSourceFileFullMetadata(srcFile)

                val needUpdate = when {
                    !updatedHeader.directDependencies.allFiles { lib, file, dependencies ->
                        cachedHeader.directDependencies[lib, file]?.keys?.containsAll(dependencies.keys) ?: dependencies.isEmpty()
                    } -> true

                    !updatedHeader.inverseDependencies.allFiles { lib, file, invDependencies ->
                        cachedHeader.inverseDependencies[lib, file]?.containsAll(invDependencies) ?: invDependencies.isEmpty()
                    } -> true

                    else -> false
                }
                if (needUpdate) {
                    cachedHeader.directDependencies.forEachFile { lib, file, dependencies ->
                        val updatedDependencies = updatedHeader.directDependencies[lib, file]
                        if (updatedDependencies != null) {
                            updatedDependencies += dependencies
                        } else {
                            updatedHeader.directDependencies[lib, file] = HashMap(dependencies)
                        }
                    }
                    cachedHeader.inverseDependencies.forEachFile { lib, file, dependencies ->
                        val updatedDependencies = updatedHeader.inverseDependencies[lib, file]
                        if (updatedDependencies != null) {
                            updatedDependencies += dependencies
                        } else {
                            updatedHeader.inverseDependencies[lib, file] = HashSet(dependencies)
                        }
                    }
                    incrementalCache.updateSourceFileMetadata(srcFile, updatedHeader)
                }
            }
        }

        fun buildCacheArtifacts(
            jsIrLinker: JsIrLinker,
            loadedFragments: Map<KotlinLibraryFile, IrModuleFragment>
        ): Map<KotlinLibraryFile, IncrementalCacheArtifact> {
            return mapLibrariesInOrder { libFile ->
                val incrementalCache = getLibIncrementalCache(libFile)
                val libFragment = loadedFragments[libFile] ?: notFoundIcError("loaded fragment", libFile)
                val moduleDeserializer = jsIrLinker.moduleDeserializer(libFragment.descriptor)

                val signatureToIndexMapping = moduleDeserializer.fileDeserializers().associate {
                    KotlinSourceFile(it.file) to it.symbolDeserializer.signatureDeserializer.signatureToIndexMapping()
                }
                libFile to incrementalCache.buildIncrementalCacheArtifact(signatureToIndexMapping)
            }.toMap()
        }
    }

    private fun commitCacheAndBuildModuleArtifacts(
        incrementalCacheArtifacts: Map<KotlinLibraryFile, IncrementalCacheArtifact>,
        moduleNames: Map<KotlinLibraryFile, String>,
        rebuiltFileFragments: KotlinSourceFileMap<JsIrProgramFragment>
    ): List<ModuleArtifact> {
        stopwatch.startNext("Incremental cache - committing artifacts")

        val artifacts = mapLibrariesInOrder { libFile ->
            val incrementalCacheArtifact = incrementalCacheArtifacts[libFile] ?: notFoundIcError("cache artifact", libFile)
            val library = libraries[libFile] ?: notFoundIcError("library", libFile)
            incrementalCacheArtifact.buildModuleArtifactAndCommitCache(
                moduleName = moduleNames[libFile] ?: notFoundIcError("module name", libFile),
                externalModuleName = library.jsOutputName,
                rebuiltFileFragments = rebuiltFileFragments[libFile] ?: emptyMap()
            )
        }

        stopwatch.stop()
        return artifacts
    }

    private fun compileDirtyFiles(
        compilerForIC: JsIrCompilerICInterface,
        loadedFragments: Map<KotlinLibraryFile, IrModuleFragment>,
        dirtyFiles: Map<KotlinLibraryFile, Set<KotlinSourceFile>>
    ): MutableList<Triple<KotlinLibraryFile, KotlinSourceFile, () -> JsIrProgramFragment>> {
        stopwatch.startNext("Processing IR - lowering")

        val dirtyFilesForCompiling = mutableListOf<IrFile>()
        val dirtyFilesForRestoring = mutableListOf<Pair<KotlinLibraryFile, KotlinSourceFile>>()
        for ((libFile, libFragment) in loadedFragments) {
            val dirtySrcFiles = dirtyFiles[libFile] ?: continue
            for (irFile in libFragment.files) {
                val srcFile = KotlinSourceFile(irFile)
                if (srcFile in dirtySrcFiles) {
                    dirtyFilesForCompiling += irFile
                    dirtyFilesForRestoring += libFile to srcFile
                }
            }
        }

        val fragmentGenerators = compilerForIC.compile(loadedFragments.values, dirtyFilesForCompiling, mainArguments)

        stopwatch.stop()
        return dirtyFilesForRestoring.mapIndexedTo(ArrayList(dirtyFilesForRestoring.size)) { i, libFileAndSrcFile ->
            Triple(libFileAndSrcFile.first, libFileAndSrcFile.second, fragmentGenerators[i])
        }
    }

    private data class IrForDirtyFilesAndCompiler(
        val incrementalCacheArtifacts: Map<KotlinLibraryFile, IncrementalCacheArtifact>,
        val loadedFragments: Map<KotlinLibraryFile, IrModuleFragment>,
        val dirtyFiles: Map<KotlinLibraryFile, Set<KotlinSourceFile>>,
        val irCompiler: JsIrCompilerICInterface
    )

    private fun loadIrForDirtyFilesAndInitCompiler(): IrForDirtyFilesAndCompiler {
        dirtyFileStats.clear()

        stopwatch.startNext("Incremental cache - initializing")
        val updater = CacheUpdaterInternal()

        stopwatch.startNext("Modified files - checking hashes and collecting")
        val modifiedFiles = updater.loadModifiedFiles()

        stopwatch.startNext("Modified files - collecting exported signatures")
        val dirtyFileExports = updater.collectExportedSymbolsForDirtyFiles(modifiedFiles)

        stopwatch.startNext("Modified files - loading and linking IR")
        val jsIrLinkerLoader = JsIrLinkerLoader(compilerConfiguration, mainLibrary, dependencyGraph, irFactory())
        var loadedIr = jsIrLinkerLoader.loadIr(dirtyFileExports)

        var iterations = 0
        var lastDirtyFiles: KotlinSourceFileMap<KotlinSourceFileExports> = dirtyFileExports

        while (true) {
            stopwatch.startNext("Dependencies ($iterations) - calculating transitive hashes for inline functions")
            updater.signatureHashCalculator.updateInlineFunctionTransitiveHashes(loadedIr.loadedFragments.values)

            stopwatch.startNext("Dependencies ($iterations) - updating a dependency graph")
            val dirtyMetadata = updater.rebuildDirtySourceMetadata(loadedIr.linker, loadedIr.loadedFragments, lastDirtyFiles)

            stopwatch.startNext("Dependencies ($iterations) - collecting files with updated exports and imports")
            val filesWithModifiedExportsOrImports = updater.collectFilesWithModifiedExportsAndImports(dirtyMetadata)

            stopwatch.startNext("Dependencies ($iterations) - collecting exported signatures for files with updated exports and imports")
            val filesToRebuild = updater.collectFilesToRebuildSignatures(filesWithModifiedExportsOrImports)

            if (filesToRebuild.isEmpty()) {
                break
            }

            lastDirtyFiles = filesToRebuild
            dirtyFileExports.copyFilesFrom(filesToRebuild)

            stopwatch.startNext("Dependencies ($iterations) - loading and linking IR for files with modified exports and imports")
            loadedIr = jsIrLinkerLoader.loadIr(filesToRebuild)
            iterations++
        }

        if (iterations != 0) {
            stopwatch.startNext("Loading and linking all IR")
            loadedIr = jsIrLinkerLoader.loadIr(dirtyFileExports)
        }

        stopwatch.startNext("Processing IR - initializing backend context")
        val mainModule = loadedIr.loadedFragments[mainLibraryFile] ?: notFoundIcError("main lib loaded fragment", mainLibraryFile)
        val compilerForIC = compilerInterfaceFactory.createCompilerForIC(mainModule, compilerConfiguration)

        // Load declarations referenced during `context` initialization
        loadedIr.linker.loadUnboundSymbols(true)

        val dirtyFiles = dirtyFileExports.entries.associateTo(HashMap(dirtyFileExports.size)) { it.key to HashSet(it.value.keys) }

        stopwatch.startNext("Processing IR - updating intrinsics and builtins dependencies")
        updater.updateStdlibIntrinsicDependencies(loadedIr.linker, mainModule, loadedIr.loadedFragments, dirtyFiles)

        stopwatch.startNext("Incremental cache - building artifacts")
        val incrementalCachesArtifacts = updater.buildCacheArtifacts(loadedIr.linker, loadedIr.loadedFragments)

        stopwatch.stop()
        return IrForDirtyFilesAndCompiler(incrementalCachesArtifacts, loadedIr.loadedFragments, dirtyFiles, compilerForIC)
    }

    private data class FragmentGenerators(
        val incrementalCacheArtifacts: Map<KotlinLibraryFile, IncrementalCacheArtifact>,
        val moduleNames: Map<KotlinLibraryFile, String>,
        val generators: MutableList<Triple<KotlinLibraryFile, KotlinSourceFile, () -> JsIrProgramFragment>>
    )

    private fun loadIrAndMakeIrFragmentGenerators(): FragmentGenerators {
        val (incrementalCachesArtifacts, irFragments, dirtyFiles, irCompiler) = loadIrForDirtyFilesAndInitCompiler()

        val moduleNames = irFragments.entries.associate { it.key to it.value.name.asString() }

        val rebuiltFragmentGenerators = compileDirtyFiles(irCompiler, irFragments, dirtyFiles)

        return FragmentGenerators(incrementalCachesArtifacts, moduleNames, rebuiltFragmentGenerators)
    }

    private fun generateIrFragments(
        generators: MutableList<Triple<KotlinLibraryFile, KotlinSourceFile, () -> JsIrProgramFragment>>
    ): KotlinSourceFileMap<JsIrProgramFragment> {
        stopwatch.startNext("Processing IR - generating program fragments")
        val rebuiltFragments = KotlinSourceFileMutableMap<JsIrProgramFragment>()
        while (generators.isNotEmpty()) {
            val (libFile, srcFile, fragmentGenerator) = generators.removeFirst()
            rebuiltFragments[libFile, srcFile] = fragmentGenerator()
        }
        stopwatch.stop()
        return rebuiltFragments
    }

    fun actualizeCaches(): List<ModuleArtifact> {
        val (incrementalCachesArtifacts, moduleNames, generators) = loadIrAndMakeIrFragmentGenerators()

        val rebuiltFragments = generateIrFragments(generators)

        return commitCacheAndBuildModuleArtifacts(incrementalCachesArtifacts, moduleNames, rebuiltFragments)
    }
}

// Used for tests only
fun rebuildCacheForDirtyFiles(
    library: KotlinLibrary,
    configuration: CompilerConfiguration,
    dependencyGraph: Map<KotlinLibrary, List<KotlinLibrary>>,
    dirtyFiles: Collection<String>?,
    irFactory: IrFactory,
    exportedDeclarations: Set<FqName>,
    mainArguments: List<String>?,
): Pair<IrModuleFragment, List<Pair<IrFile, JsIrProgramFragment>>> {
    val emptyMetadata = object : KotlinSourceFileExports() {
        override val inverseDependencies = KotlinSourceFileMap<Set<IdSignature>>(emptyMap())
    }

    val libFile = KotlinLibraryFile(library)
    val dirtySrcFiles = dirtyFiles?.map { KotlinSourceFile(it) } ?: KotlinLibraryHeader(library).sourceFiles
    val modifiedFiles = mapOf(libFile to dirtySrcFiles.associateWith { emptyMetadata })

    val jsIrLoader = JsIrLinkerLoader(configuration, library, dependencyGraph, irFactory)
    val (jsIrLinker, irModules) = jsIrLoader.loadIr(KotlinSourceFileMap<KotlinSourceFileExports>(modifiedFiles), true)

    val currentIrModule = irModules[libFile] ?: notFoundIcError("loaded fragment", libFile)
    val dirtyIrFiles = dirtyFiles?.let {
        val files = it.toSet()
        currentIrModule.files.filter { irFile -> irFile.fileEntry.name in files }
    } ?: currentIrModule.files

    val compilerWithIC = JsIrCompilerWithIC(currentIrModule, configuration, JsGenerationGranularity.PER_MODULE, exportedDeclarations)

    // Load declarations referenced during `context` initialization
    jsIrLinker.loadUnboundSymbols(true)

    val fragments = compilerWithIC.compile(irModules.values, dirtyIrFiles, mainArguments).map { it() }

    return currentIrModule to dirtyIrFiles.zip(fragments)
}
