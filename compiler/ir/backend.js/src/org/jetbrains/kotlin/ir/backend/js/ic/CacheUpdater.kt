/* * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.ic

import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.backend.js.*
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.JsIrLinker
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.JsIrFragmentAndBinaryAst
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.util.resolveFakeOverride
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.konan.properties.propertyList
import org.jetbrains.kotlin.library.KLIB_PROPERTY_DEPENDS
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.name.FqName
import java.io.File
import java.util.EnumSet


fun interface CacheExecutor {
    fun execute(
        mainModule: IrModuleFragment,
        allModules: Collection<IrModuleFragment>,
        deserializer: JsIrLinker,
        configuration: CompilerConfiguration,
        dirtyFiles: Collection<IrFile>,
        exportedDeclarations: Set<FqName>,
        mainArguments: List<String>?
    ): List<JsIrFragmentAndBinaryAst>
}

enum class DirtyFileState(val str: String) {
    ADDED_FILE("added file"),
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
    private val executor: CacheExecutor
) {
    private val signatureHashCalculator = IdSignatureHashCalculator()

    private val libraries = loadLibraries(allModules)
    private val dependencyGraph = buildDependenciesGraph(libraries)
    private val configHash = compilerConfiguration.configHashForIC()

    private val cacheMap = libraries.values.zip(icCachePaths).toMap()

    private val mainLibraryFile = KotlinLibraryFile(File(mainModule).canonicalPath)
    private val mainLibrary = libraries[mainLibraryFile] ?: notFoundIcError("main library", mainLibraryFile)

    private val incrementalCaches = libraries.entries.associate { (libFile, lib) ->
        val cachePath = cacheMap[lib] ?: notFoundIcError("cache path", KotlinLibraryFile(lib))
        libFile to IncrementalCache(lib, cachePath)
    }

    private val dirtyFileStats = KotlinSourceFileMutableMap<EnumSet<DirtyFileState>>()

    fun getDirtyFileStats(): KotlinSourceFileMap<EnumSet<DirtyFileState>> = dirtyFileStats

    private fun MutableMap<KotlinSourceFile, EnumSet<DirtyFileState>>.addDirtFileStat(srcFile: KotlinSourceFile, state: DirtyFileState) {
        when (val stats = this[srcFile]) {
            null -> this[srcFile] = EnumSet.of(state)
            else -> stats.add(state)
        }
    }

    private fun getLibIncrementalCache(libFile: KotlinLibraryFile) =
        incrementalCaches[libFile] ?: notFoundIcError("incremental cache", libFile)

    private fun loadLibraries(allModules: Collection<String>): Map<KotlinLibraryFile, KotlinLibrary> {
        val allResolvedDependencies = jsResolveLibraries(
            allModules,
            compilerConfiguration[JSConfigurationKeys.REPOSITORIES] ?: emptyList(),
            compilerConfiguration[IrMessageLogger.IR_MESSAGE_LOGGER].toResolverLogger()
        )

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

    private class DirtyFileExports(
        override val inverseDependencies: KotlinSourceFileMutableMap<Set<IdSignature>> = KotlinSourceFileMutableMap()
    ) : KotlinSourceFileExports() {
        override fun getExportedSignatures(): Set<IdSignature> = allExportedSignatures

        val allExportedSignatures = mutableSetOf<IdSignature>()
    }

    private class DirtyFileMetadata(
        val maybeImportedSignatures: Collection<IdSignature>,

        val oldDirectDependencies: KotlinSourceFileMap<*>,

        override val inverseDependencies: KotlinSourceFileMutableMap<MutableSet<IdSignature>> = KotlinSourceFileMutableMap(),
        override val directDependencies: KotlinSourceFileMutableMap<MutableMap<IdSignature, ICHash>> = KotlinSourceFileMutableMap(),
    ) : KotlinSourceFileMetadata() {
        fun addInverseDependency(lib: KotlinLibraryFile, src: KotlinSourceFile, signature: IdSignature) =
            when (val signatures = inverseDependencies[lib, src]) {
                null -> inverseDependencies[lib, src] = mutableSetOf(signature)
                else -> signatures += signature
            }

        fun addDirectDependency(lib: KotlinLibraryFile, src: KotlinSourceFile, signature: IdSignature, hash: ICHash) =
            when (val signatures = directDependencies[lib, src]) {
                null -> directDependencies[lib, src] = mutableMapOf(signature to hash)
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

    private fun addFilesWithRemovedDependencies(
        modifiedFiles: KotlinSourceFileMap<KotlinSourceFileMetadata>, removedFiles: KotlinSourceFileMap<KotlinSourceFileMetadata>
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

    private fun loadModifiedFiles(): KotlinSourceFileMap<KotlinSourceFileMetadata> {
        val removedFilesMetadata = mutableMapOf<KotlinLibraryFile, Map<KotlinSourceFile, KotlinSourceFileMetadata>>()

        val modifiedFiles = KotlinSourceFileMap(incrementalCaches.entries.associate { (lib, cache) ->
            val (dirtyFiles, removedFiles, newFiles) = cache.collectModifiedFiles(configHash)

            val fileStats by lazy(LazyThreadSafetyMode.NONE) { dirtyFileStats.getOrPutFiles(lib) }
            newFiles.forEach { fileStats.addDirtFileStat(it, DirtyFileState.ADDED_FILE) }
            removedFiles.forEach { fileStats.addDirtFileStat(it.key, DirtyFileState.REMOVED_FILE) }
            dirtyFiles.forEach {
                if (it.key !in newFiles) {
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

    private fun collectExportedSymbolsForDirtyFiles(
        dirtyFiles: KotlinSourceFileMap<KotlinSourceFileMetadata>
    ): KotlinSourceFileMutableMap<KotlinSourceFileExports> {
        val exportedSymbols = KotlinSourceFileMutableMap<KotlinSourceFileExports>()

        for ((libFile, srcFiles) in dirtyFiles) {
            val exportedSymbolFiles = mutableMapOf<KotlinSourceFile, KotlinSourceFileExports>()
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

    private fun resolveFakeOverrideFunction(symbol: IrSymbol): IdSignature? {
        return (symbol.owner as? IrSimpleFunction)?.let { overridable ->
            if (overridable.isFakeOverride) {
                overridable.resolveFakeOverride()?.symbol?.signature
            } else {
                null
            }
        }
    }

    private fun collectImplementedSymbol(deserializedSymbols: Map<IdSignature, IrSymbol>): Map<IdSignature, IrSymbol> {
        return buildMap(deserializedSymbols.size) {
            for ((signature, symbol) in deserializedSymbols) {
                put(signature, symbol)

                fun <T> addSymbol(decl: T): Boolean where T : IrDeclarationWithVisibility, T : IrSymbolOwner {
                    when (decl.visibility) {
                        DescriptorVisibilities.LOCAL -> return false
                        DescriptorVisibilities.PRIVATE -> return false
                        DescriptorVisibilities.PRIVATE_TO_THIS -> return false
                    }

                    val sig = decl.symbol.signature
                    if (sig != null && sig !in deserializedSymbols) {
                        return put(sig, decl.symbol) == null
                    }
                    return false
                }

                fun addNestedDeclarations(irClass: IrClass) {
                    for (decl in irClass.declarations) {
                        when (decl) {
                            is IrSimpleFunction -> addSymbol(decl)
                            is IrProperty -> {
                                decl.getter?.let(::addSymbol)
                                decl.setter?.let(::addSymbol)
                            }
                            is IrClass -> {
                                if (addSymbol(decl)) {
                                    addNestedDeclarations(decl)
                                }
                            }
                        }
                    }
                }

                (symbol.owner as? IrClass)?.let(::addNestedDeclarations)
            }
        }
    }

    private fun rebuildDirtySourceMetadata(
        jsIrLinker: JsIrLinker,
        loadedFragments: Map<KotlinLibraryFile, IrModuleFragment>,
        dirtySrcFiles: KotlinSourceFileMap<KotlinSourceFileExports>,
    ): KotlinSourceFileMap<DirtyFileMetadata> {
        val idSignatureToFile = mutableMapOf<IdSignature, Pair<KotlinLibraryFile, KotlinSourceFile>>()
        val updatedMetadata = KotlinSourceFileMutableMap<DirtyFileMetadata>()

        for ((lib, irModule) in loadedFragments) {
            val moduleDeserializer = jsIrLinker.moduleDeserializer(irModule.descriptor)
            val incrementalCache = getLibIncrementalCache(lib)
            for (fileDeserializer in moduleDeserializer.fileDeserializers()) {
                val libSrcFile = KotlinSourceFile(fileDeserializer.file)

                val reachableSignatures = fileDeserializer.symbolDeserializer.signatureDeserializer.signatureToIndexMapping()
                val maybeImportedSignatures = reachableSignatures.keys.toMutableSet()
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
                        signatureHashCalculator.addHashForSignatureIfNotExist(signature, symbol)
                        idSignatureToFile[signature] = lib to libSrcFile
                    }
                }

                if (dirtySrcFiles[lib, libSrcFile] != null) {
                    val metadata = incrementalCache.fetchSourceFileFullMetadata(libSrcFile)
                    updatedMetadata[lib, libSrcFile] = DirtyFileMetadata(maybeImportedSignatures, metadata.directDependencies)
                }
            }
        }

        for ((libFile, srcFiles) in updatedMetadata) {
            val libDirtySrcFiles = dirtySrcFiles[libFile] ?: continue
            for ((srcFile, internalHeader) in srcFiles) {
                val dirtySrcFile = libDirtySrcFiles[srcFile] ?: continue
                dirtySrcFile.inverseDependencies.forEachFile { dependentLibFile, dependentSrcFile, signatures ->
                    signatures.forEach {
                        val (dependencyLib, dependencyFile) = idSignatureToFile[it] ?: (libFile to srcFile)
                        updatedMetadata[dependencyLib, dependencyFile]?.also { dependencyMetadata ->
                            dependencyMetadata.addInverseDependency(dependentLibFile, dependentSrcFile, it)
                        }
                    }
                }

                for (importedSignature in internalHeader.maybeImportedSignatures) {
                    val (dependencyLib, dependencyFile) = idSignatureToFile[importedSignature] ?: continue
                    signatureHashCalculator[importedSignature]?.also { signatureHash ->
                        internalHeader.addDirectDependency(dependencyLib, dependencyFile, importedSignature, signatureHash)
                    } ?: notFoundIcError("signature $importedSignature hash", dependencyLib, dependencyFile)

                    updatedMetadata[dependencyLib, dependencyFile]?.also { dependencyMetadata ->
                        dependencyMetadata.addInverseDependency(libFile, srcFile, importedSignature)
                    }
                }
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

    private fun KotlinSourceFileMutableMap<UpdatedDependenciesMetadata>.addNewMetadata(
        libFile: KotlinLibraryFile, srcFile: KotlinSourceFile, oldMetadata: KotlinSourceFileMetadata
    ) = this[libFile, srcFile] ?: UpdatedDependenciesMetadata(oldMetadata).also {
        this[libFile, srcFile] = it
    }

    private fun KotlinSourceFileMutableMap<UpdatedDependenciesMetadata>.addDependenciesWithUpdatedSignatures(
        libFile: KotlinLibraryFile, srcFile: KotlinSourceFile, srcFileMetadata: DirtyFileMetadata
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
        libFile: KotlinLibraryFile, srcFile: KotlinSourceFile, srcFileMetadata: DirtyFileMetadata
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
        libFile: KotlinLibraryFile, srcFile: KotlinSourceFile, srcFileMetadata: DirtyFileMetadata
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
                        newMetadata.directDependencies[libFile, srcFile] = newSignatures.associateWith {
                            signatureHashCalculator[it] ?: notFoundIcError("signature $it hash", libFile, srcFile)
                        }
                    }
                }
            }
        }
    }

    private fun collectFilesWithModifiedExportsOrInlineImports(
        loadedDirtyFiles: KotlinSourceFileMap<DirtyFileMetadata>
    ): KotlinSourceFileMap<UpdatedDependenciesMetadata> {
        val filesWithModifiedExports = KotlinSourceFileMutableMap<UpdatedDependenciesMetadata>()

        loadedDirtyFiles.forEachFile { libFile, srcFile, srcFileMetadata ->
            filesWithModifiedExports.addDependenciesWithUpdatedSignatures(libFile, srcFile, srcFileMetadata)
            filesWithModifiedExports.addDependenciesWithRemovedInverseDependencies(libFile, srcFile, srcFileMetadata)
            filesWithModifiedExports.addDependentsWithUpdatedImports(libFile, srcFile, srcFileMetadata)
        }

        return filesWithModifiedExports
    }

    private fun collectFilesToRebuildSignatures(
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

    private fun buildModuleArtifactsAndCommitCache(
        jsIrLinker: JsIrLinker,
        loadedFragments: Map<KotlinLibraryFile, IrModuleFragment>,
        rebuiltFileFragments: List<JsIrFragmentAndBinaryAst>
    ): List<ModuleArtifact> {
        val fragmentToLibName = loadedFragments.entries.associate { it.value to it.key }

        val rebuiltSrcFiles = rebuiltFileFragments.groupBy {
            fragmentToLibName[it.irFile.module] ?: notFoundIcError("loaded fragment lib name", srcFile = KotlinSourceFile(it.irFile))
        }

        val visited = mutableSetOf<KotlinLibrary>()
        val artifacts = mutableListOf<ModuleArtifact>()
        fun addArtifact(lib: KotlinLibrary) {
            if (visited.add(lib)) {
                dependencyGraph[lib]?.forEach(::addArtifact)

                val libFile = KotlinLibraryFile(lib)
                val incrementalCache = getLibIncrementalCache(libFile)
                val libFragment = loadedFragments[libFile] ?: notFoundIcError("loaded fragment", libFile)
                val libRebuiltFiles = rebuiltSrcFiles[libFile]?.associateBy { KotlinSourceFile(it.irFile) } ?: emptyMap()
                val moduleDeserializer = jsIrLinker.moduleDeserializer(libFragment.descriptor)

                val signatureToIndexMapping = moduleDeserializer.fileDeserializers().associate {
                    KotlinSourceFile(it.file) to it.symbolDeserializer.signatureDeserializer.signatureToIndexMapping()
                }
                artifacts += incrementalCache.buildModuleArtifactAndCommitCache(
                    moduleName = libFragment.name.asString(),
                    rebuiltFileFragments = libRebuiltFiles,
                    signatureToIndexMapping = signatureToIndexMapping
                )
            }
        }

        addArtifact(mainLibrary)

        return artifacts
    }

    fun actualizeCaches(eventCallback: (String) -> Unit = {}): List<ModuleArtifact> {
        dirtyFileStats.clear()

        val modifiedFiles = loadModifiedFiles()
        val dirtyFileExports = collectExportedSymbolsForDirtyFiles(modifiedFiles)

        val jsIrLinkerLoader = JsIrLinkerLoader(compilerConfiguration, mainLibrary, dependencyGraph, irFactory())
        var loadedIr = jsIrLinkerLoader.loadIr(dirtyFileExports)

        eventCallback("initial loading of updated files")

        var iterations = 0
        var lastDirtyFiles: KotlinSourceFileMap<KotlinSourceFileExports> = dirtyFileExports

        while (true) {
            signatureHashCalculator.updateInlineFunctionTransitiveHashes(loadedIr.loadedFragments.values)

            val dirtyHeaders = rebuildDirtySourceMetadata(loadedIr.linker, loadedIr.loadedFragments, lastDirtyFiles)

            val filesWithModifiedExportsOrImports = collectFilesWithModifiedExportsOrInlineImports(dirtyHeaders)

            val filesToRebuild = collectFilesToRebuildSignatures(filesWithModifiedExportsOrImports)

            eventCallback("actualization iteration $iterations")
            if (filesToRebuild.isEmpty()) {
                break
            }

            loadedIr = jsIrLinkerLoader.loadIr(filesToRebuild)
            iterations++

            lastDirtyFiles = filesToRebuild
            dirtyFileExports.copyFilesFrom(filesToRebuild)
        }

        if (iterations != 0) {
            loadedIr = jsIrLinkerLoader.loadIr(dirtyFileExports)
            eventCallback("final loading of updated files")
        }

        val rebuiltFragments = executor.execute(
            mainModule = loadedIr.loadedFragments[mainLibraryFile] ?: notFoundIcError("main lib loaded fragment", mainLibraryFile),
            allModules = loadedIr.loadedFragments.values,
            deserializer = loadedIr.linker,
            configuration = compilerConfiguration,
            dirtyFiles = loadedIr.loadedFragments.flatMap { (libFile, libFragment) ->
                dirtyFileExports[libFile]?.let { libDirtyFiles ->
                    libFragment.files.filter { file -> KotlinSourceFile(file) in libDirtyFiles }
                } ?: emptyList()
            },
            exportedDeclarations = emptySet(),
            mainArguments = mainArguments
        )
        eventCallback("updated files processing (lowering)")

        val artifacts = buildModuleArtifactsAndCommitCache(loadedIr.linker, loadedIr.loadedFragments, rebuiltFragments)
        eventCallback("cache committing")

        return artifacts
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
): Pair<IrModuleFragment, List<JsIrFragmentAndBinaryAst>> {
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

    return currentIrModule to buildCacheForModuleFiles(
        mainModule = currentIrModule,
        allModules = irModules.values,
        deserializer = jsIrLinker,
        configuration = configuration,
        dirtyFiles = dirtyIrFiles,
        exportedDeclarations = exportedDeclarations,
        mainArguments = mainArguments
    )
}

fun buildCacheForModuleFiles(
    mainModule: IrModuleFragment,
    allModules: Collection<IrModuleFragment>,
    deserializer: JsIrLinker,
    configuration: CompilerConfiguration,
    dirtyFiles: Collection<IrFile>,
    exportedDeclarations: Set<FqName>,
    mainArguments: List<String>?
): List<JsIrFragmentAndBinaryAst> {
    return compileWithIC(
        mainModule = mainModule,
        allModules = allModules,
        filesToLower = dirtyFiles,
        configuration = configuration,
        deserializer = deserializer,
        mainArguments = mainArguments,
        exportedDeclarations = exportedDeclarations,
    )
}
