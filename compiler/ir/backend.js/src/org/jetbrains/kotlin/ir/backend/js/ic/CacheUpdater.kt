/* * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.ic

import org.jetbrains.kotlin.backend.common.CommonKLibResolver
import org.jetbrains.kotlin.backend.common.phaser.PhaseConfig
import org.jetbrains.kotlin.backend.common.serialization.IrInterningService
import org.jetbrains.kotlin.backend.common.serialization.cityHash64String
import org.jetbrains.kotlin.backend.common.toLogger
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.DuplicatedUniqueNameStrategy
import org.jetbrains.kotlin.config.KlibConfigurationKeys
import org.jetbrains.kotlin.config.messageCollector
import org.jetbrains.kotlin.ir.backend.js.*
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.JsGenerationGranularity
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.konan.properties.propertyList
import org.jetbrains.kotlin.library.KLIB_PROPERTY_DEPENDS
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.metadata.resolver.TopologicalLibraryOrder
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.utils.memoryOptimizedFilter
import org.jetbrains.kotlin.utils.memoryOptimizedMap
import org.jetbrains.kotlin.utils.newHashMapWithExpectedSize
import org.jetbrains.kotlin.utils.newHashSetWithExpectedSize
import java.io.File
import java.io.OutputStream
import java.nio.file.Files
import java.util.*

abstract class IrModule {
    abstract val moduleName: String
    abstract val fragments: List<IrProgramFragment>
}

abstract class IrProgramFragment

abstract class IrProgramFragments {
    abstract val mainFragment: IrProgramFragment
    abstract val exportFragment: IrProgramFragment?
    abstract fun serialize(stream: OutputStream)
}

fun interface IrCompilerICInterface {
    /**
     * It is expected that the method implementation runs a lowering pipeline
     * and produces a list of generators capable of generating JS AST fragments.
     */
    fun compile(allModules: Collection<IrModuleFragment>, dirtyFiles: Collection<IrFile>): List<() -> IrProgramFragments>
}

enum class DirtyFileState(val str: String) {
    ADDED_FILE("added file"),
    MODIFIED_IR("modified ir"),
    NON_MODIFIED_IR("non modified ir"),
    UPDATED_EXPORTS("updated exports"),
    UPDATED_IMPORTS("updated imports"),
    REMOVED_INVERSE_DEPENDS("removed inverse depends"),
    REMOVED_DIRECT_DEPENDS("removed direct depends"),
    REMOVED_FILE("removed file")
}

interface PlatformDependentICContext {
    fun createIrFactory(): IrFactory

    /**
     * It is expected that the method implementation creates a backend context and initializes all builtins and intrinsics.
     */
    fun createCompiler(mainModule: IrModuleFragment, configuration: CompilerConfiguration): IrCompilerICInterface

    fun createSrcFileArtifact(srcFilePath: String, fragments: IrProgramFragments?, astArtifact: File? = null): SrcFileArtifact

    fun createModuleArtifact(
        moduleName: String,
        fileArtifacts: List<SrcFileArtifact>,
        artifactsDir: File? = null,
        forceRebuildJs: Boolean = false,
        externalModuleName: String? = null
    ): ModuleArtifact
}

/**
 * This class is the entry point for the incremental compilation routine.
 * The most interesting params:
 * @param cacheDir - the directory where the incremental cache updater will store its caches. [CacheUpdater] maintains the directory fully.
 * @param icContext - used to create an instance of the compiler used for building dirty files.
 *
 * The main public methods are:
 *  [actualizeCaches] - performs the entire incremental compilation routine;
 *  [getDirtyFileLastStats] - retrieves stats about dirty files, explaining why the file has been marked as dirty (see [DirtyFileState]);
 *  [getStopwatchLastLaps] - retrieves the durations of the incremental compilation subroutines;
 *
 *  For a better understanding of what happens here, pay attention to [stopwatch] usages.
 *  In every place, it has a short description about the code it measures.
 */
class CacheUpdater(
    mainModule: String,
    private val allModules: Collection<String>,
    private val mainModuleFriends: Collection<String>,
    cacheDir: String,
    private val compilerConfiguration: CompilerConfiguration,
    private val icContext: PlatformDependentICContext,
    checkForClassStructuralChanges: Boolean = false,
) {
    private val stopwatch = StopwatchIC()

    private val dirtyFileStats = KotlinSourceFileMutableMap<EnumSet<DirtyFileState>>()

    private val mainLibraryFile = KotlinLibraryFile(File(mainModule).canonicalPath)

    private val icHasher = ICHasher(checkForClassStructuralChanges)

    private val irInterner = IrInterningService()

    private val cacheRootDir = run {
        val configHash = icHasher.calculateConfigHash(compilerConfiguration)
        File(cacheDir, "version.${configHash.hash.lowBytes.toString(Character.MAX_RADIX)}")
    }

    fun getDirtyFileLastStats(): KotlinSourceFileMap<EnumSet<DirtyFileState>> = dirtyFileStats

    fun getStopwatchLastLaps() = stopwatch.laps

    private fun MutableMap<KotlinSourceFile, EnumSet<DirtyFileState>>.addDirtFileStat(srcFile: KotlinSourceFile, state: DirtyFileState) {
        when (val stats = this[srcFile]) {
            null -> this[srcFile] = EnumSet.of(state)
            else -> stats.add(state)
        }
    }

    private inner class CacheUpdaterInternal {
        val signatureHashCalculator = IdSignatureHashCalculator(icHasher)

        // libraries in topological order: [stdlib, ..., main]
        val libraryDependencies = stopwatch.measure("Resolving and loading klib dependencies") {
            val zipAccessor = compilerConfiguration.get(JSConfigurationKeys.ZIP_FILE_SYSTEM_ACCESSOR)
            val allResolvedDependencies = CommonKLibResolver.resolve(
                allModules,
                compilerConfiguration.messageCollector.toLogger(),
                zipAccessor,
                duplicatedUniqueNameStrategy = compilerConfiguration.get(
                    KlibConfigurationKeys.DUPLICATED_UNIQUE_NAME_STRATEGY,
                    DuplicatedUniqueNameStrategy.DENY
                ),
            )

            val libraries = allResolvedDependencies.getFullList(TopologicalLibraryOrder).let { resolvedLibraries ->
                val mainLibraryIndex = resolvedLibraries.indexOfLast {
                    KotlinLibraryFile(it) == mainLibraryFile
                }.takeIf { it >= 0 } ?: notFoundIcError("main library", mainLibraryFile)

                when (mainLibraryIndex) {
                    resolvedLibraries.lastIndex -> resolvedLibraries
                    else -> resolvedLibraries.filterIndexedTo(ArrayList(resolvedLibraries.size)) { index, _ ->
                        index != mainLibraryIndex
                    }.apply { add(resolvedLibraries[mainLibraryIndex]) }
                }
            }

            val nameToKotlinLibrary = libraries.associateBy { it.moduleName }

            libraries.associateWith {
                it.manifestProperties.propertyList(KLIB_PROPERTY_DEPENDS, escapeInQuotes = true).memoryOptimizedMap { depName ->
                    nameToKotlinLibrary[depName] ?: notFoundIcError("library $depName")
                }
            }
        }

        val mainModuleFriendLibraries = libraryDependencies.keys.let { libs ->
            val friendPaths = mainModuleFriends.mapTo(newHashSetWithExpectedSize(mainModuleFriends.size)) { File(it).canonicalPath }
            libs.memoryOptimizedFilter { it.libraryFile.canonicalPath in friendPaths }
        }

        private val incrementalCaches = libraryDependencies.keys.associate { lib ->
            val libFile = KotlinLibraryFile(lib)
            val file = File(libFile.path)
            val pathHash = file.absolutePath.cityHash64String()
            val libraryCacheDir = File(cacheRootDir, "${file.name}.$pathHash")
            libFile to IncrementalCache(KotlinLoadedLibraryHeader(lib, irInterner), libraryCacheDir)
        }

        private val removedIncrementalCaches = buildList {
            if (cacheRootDir.isDirectory) {
                val availableCaches = incrementalCaches.values.mapTo(newHashSetWithExpectedSize(incrementalCaches.size)) { it.cacheDir }
                val allDirs = Files.walk(cacheRootDir.toPath(), 1).map { it.toFile() }
                allDirs.filter { it != cacheRootDir && it !in availableCaches }.forEach { removedCacheDir ->
                    add(IncrementalCache(KotlinRemovedLibraryHeader(removedCacheDir), removedCacheDir))
                }
            }
        }

        private fun getLibIncrementalCache(libFile: KotlinLibraryFile) =
            incrementalCaches[libFile] ?: notFoundIcError("incremental cache", libFile)

        private fun addFilesWithRemovedDependencies(
            modifiedFiles: KotlinSourceFileMutableMap<KotlinSourceFileMetadata>,
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

            modifiedFiles.copyFilesFrom(extraModifiedLibFiles)
            return modifiedFiles
        }

        fun loadModifiedFiles(): KotlinSourceFileMap<KotlinSourceFileMetadata> {
            val removedFilesMetadata = hashMapOf<KotlinLibraryFile, Map<KotlinSourceFile, KotlinSourceFileMetadata>>()

            fun collectDirtyFiles(lib: KotlinLibraryFile, cache: IncrementalCache): MutableMap<KotlinSourceFile, KotlinSourceFileMetadata> {
                val (addedFiles, removedFiles, modifiedFiles, nonModifiedFiles) = cache.collectModifiedFiles()

                val fileStats by lazy(LazyThreadSafetyMode.NONE) { dirtyFileStats.getOrPutFiles(lib) }
                addedFiles.forEach { fileStats.addDirtFileStat(it, DirtyFileState.ADDED_FILE) }
                removedFiles.forEach { fileStats.addDirtFileStat(it.key, DirtyFileState.REMOVED_FILE) }
                modifiedFiles.forEach { fileStats.addDirtFileStat(it.key, DirtyFileState.MODIFIED_IR) }
                nonModifiedFiles.forEach { fileStats.addDirtFileStat(it, DirtyFileState.NON_MODIFIED_IR) }

                if (removedFiles.isNotEmpty()) {
                    removedFilesMetadata[lib] = removedFiles
                }

                return addedFiles.associateWithTo(modifiedFiles.toMutableMap()) { KotlinSourceFileMetadataNotExist }
            }

            for (cache in removedIncrementalCaches) {
                val libFile = cache.libraryFileFromHeader ?: notFoundIcError("removed library name; cache dir: ${cache.cacheDir}")
                val dirtyFiles = collectDirtyFiles(libFile, cache)
                if (dirtyFiles.isNotEmpty()) {
                    icError("unexpected dirty file", libFile, dirtyFiles.keys.first())
                }
            }

            val dirtyFiles = incrementalCaches.entries.associateTo(hashMapOf()) { (lib, cache) ->
                lib to collectDirtyFiles(lib, cache)
            }

            return addFilesWithRemovedDependencies(KotlinSourceFileMutableMap(dirtyFiles), KotlinSourceFileMap(removedFilesMetadata))
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

        fun collectStubbedSignatures(): Set<IdSignature> {
            val stubbedSignatures = hashSetOf<IdSignature>()
            for (cache in incrementalCaches.values) {
                val fileStubbedSignatures = cache.collectFilesWithStubbedSignatures()
                for (signatures in fileStubbedSignatures.values) {
                    stubbedSignatures += signatures
                }
            }
            return stubbedSignatures
        }

        private fun KotlinSourceFileMutableMap<DirtyFileMetadata>.getExportedSignaturesAndAddMetadata(
            symbolProviders: List<FileSignatureProvider>,
            libFile: KotlinLibraryFile,
            dirtySrcFiles: Set<KotlinSourceFile>
        ): Map<IdSignature, IdSignatureSource> {
            val idSignatureToFile = hashMapOf<IdSignature, IdSignatureSource>()
            val incrementalCache = getLibIncrementalCache(libFile)
            for (fileSymbolProvider in symbolProviders) {
                val maybeImportedSignatures = fileSymbolProvider.getReachableSignatures().toHashSet()
                val implementedSymbols = fileSymbolProvider.getImplementedSymbols()
                for ((signature, symbol) in implementedSymbols) {
                    var symbolCanBeExported = maybeImportedSignatures.remove(signature)
                    resolveFakeOverrideFunction(symbol)?.let { resolvedSignature ->
                        if (resolvedSignature !in implementedSymbols) {
                            maybeImportedSignatures.add(resolvedSignature)
                        }
                        symbolCanBeExported = true
                    }
                    if (symbolCanBeExported) {
                        idSignatureToFile[signature] = IdSignatureSource(libFile, fileSymbolProvider, symbol)
                    }
                }

                val libSrcFile = fileSymbolProvider.srcFile
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
                val dependency = idSignatureToFile[importedSignature] ?: continue
                signatureHashCalculator[importedSignature]?.also { signatureHash ->
                    addDirectDependency(dependency.lib, dependency.srcFile, importedSignature, signatureHash)
                } ?: notFoundIcError("signature $importedSignature hash", dependency.lib, dependency.srcFile)

                updatedMetadata[dependency.lib, dependency.srcFile]?.also { dependencyMetadata ->
                    dependencyMetadata.addInverseDependency(libFile, srcFile, importedSignature)
                }
            }
        }

        fun rebuildDirtySourceMetadata(
            loadedIr: LoadedJsIr,
            dirtySrcFiles: KotlinSourceFileMap<KotlinSourceFileExports>,
        ): KotlinSourceFileMap<DirtyFileMetadata> {
            val idSignatureToFile = hashMapOf<IdSignature, IdSignatureSource>()
            val updatedMetadata = KotlinSourceFileMutableMap<DirtyFileMetadata>()

            for (lib in loadedIr.loadedFragments.keys) {
                val libDirtySrcFiles = dirtySrcFiles[lib]?.keys ?: emptySet()
                val symbolProviders = loadedIr.getSignatureProvidersForLib(lib)
                idSignatureToFile += updatedMetadata.getExportedSignaturesAndAddMetadata(symbolProviders, lib, libDirtySrcFiles)
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
                            val dependencyFile = signatureSrc?.srcFile ?: srcFile
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
                        // ignore if the dependent file is already dirty
                        dependentSrcMetadata is DirtyFileMetadata -> continue

                        // ignore if the dependent file imports have been modified, the metadata for it will be rebuilt later
                        this[dependentLibFile, dependentSrcFile]?.importedSignaturesState == ImportedSignaturesState.MODIFIED -> continue

                        // update metadata if the direct dependencies have been modified
                        dependentSignatures.any { signatureHashCalculator[it.key] != it.value } -> {
                            val newMetadata = addNewMetadata(dependentLibFile, dependentSrcFile, dependentSrcMetadata)
                            newMetadata.importedSignaturesState = ImportedSignaturesState.MODIFIED
                        }

                        // update metadata if the signature set of the direct dependencies has been updated
                        dependentSignatures.keys != newSignatures -> {
                            val newMetadata = addNewMetadata(dependentLibFile, dependentSrcFile, dependentSrcMetadata)

                            if (newMetadata.importedSignaturesState == ImportedSignaturesState.UNKNOWN) {
                                val isNonModified = dependentSrcMetadata.directDependencies.allFiles { _, _, signatures ->
                                    signatures.all {
                                        val newHash = signatureHashCalculator[it.key]
                                        // a new hash may be not calculated for the non-loaded symbols, it is ok
                                        newHash == null || newHash == it.value
                                    }
                                }
                                newMetadata.importedSignaturesState = if (isNonModified) {
                                    ImportedSignaturesState.NON_MODIFIED
                                } else {
                                    ImportedSignaturesState.MODIFIED
                                }
                            }

                            // if imports have been modified, metadata for the file will be rebuilt later,
                            // so if the imports haven't been modified, update the metadata manually
                            if (newMetadata.importedSignaturesState == ImportedSignaturesState.NON_MODIFIED) {
                                val newDirectDependencies = newSignatures.associateWithTo(newHashMapWithExpectedSize(newSignatures.size)) {
                                    signatureHashCalculator[it] ?: notFoundIcError("signature $it hash", libFile, srcFile)
                                }
                                newMetadata.directDependencies[libFile, srcFile] = newDirectDependencies
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
            filesWithModifiedExportsOrImports: KotlinSourceFileMap<UpdatedDependenciesMetadata>
        ): KotlinSourceFileMap<KotlinSourceFileExports> {
            val libFilesToRebuild = KotlinSourceFileMutableMap<KotlinSourceFileExports>()

            for ((libFile, srcFiles) in filesWithModifiedExportsOrImports) {
                val filesToRebuild by lazy(LazyThreadSafetyMode.NONE) { libFilesToRebuild.getOrPutFiles(libFile) }
                val fileStats by lazy(LazyThreadSafetyMode.NONE) { dirtyFileStats.getOrPutFiles(libFile) }
                val cache = getLibIncrementalCache(libFile)

                for ((srcFile, srcFileMetadata) in srcFiles) {
                    val isExportedSignatureUpdated = srcFileMetadata.isExportedSignaturesUpdated()
                    if (isExportedSignatureUpdated || srcFileMetadata.importedSignaturesState == ImportedSignaturesState.MODIFIED) {
                        // if exported signatures or imported inline functions were modified - rebuild
                        filesToRebuild[srcFile] = srcFileMetadata
                        if (isExportedSignatureUpdated) {
                            fileStats.addDirtFileStat(srcFile, DirtyFileState.UPDATED_EXPORTS)
                        }
                        if (srcFileMetadata.importedSignaturesState == ImportedSignaturesState.MODIFIED) {
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

        fun collectFilesWithUpdatedStubbedSymbols(dirtyFiles: KotlinSourceFileMap<*>): KotlinSourceFileMap<KotlinSourceFileExports> {
            val libFiles = KotlinSourceFileMutableMap<KotlinSourceFileExports>()

            for ((libFile, cache) in incrementalCaches.entries) {
                val filesToRebuild by lazy(LazyThreadSafetyMode.NONE) { libFiles.getOrPutFiles(libFile) }
                val fileStats by lazy(LazyThreadSafetyMode.NONE) { dirtyFileStats.getOrPutFiles(libFile) }
                val alreadyDirtyFiles = dirtyFiles[libFile]?.keys ?: emptySet()
                val filesWithStubbedSignatures = cache.collectFilesWithStubbedSignatures()

                for ((srcFile, stubbedSignatures) in filesWithStubbedSignatures.entries) {
                    if (srcFile !in alreadyDirtyFiles && stubbedSignatures.any { it in signatureHashCalculator }) {
                        filesToRebuild[srcFile] = cache.fetchSourceFileFullMetadata(srcFile)
                        fileStats.addDirtFileStat(srcFile, DirtyFileState.UPDATED_IMPORTS)
                    }
                }
            }

            return libFiles
        }

        fun updateStdlibIntrinsicDependencies(
            loadedIr: LoadedJsIr,
            mainModule: IrModuleFragment,
            dirtyFiles: Map<KotlinLibraryFile, Set<KotlinSourceFile>>
        ) {
            val (stdlibFile, _) = findStdlib(mainModule, loadedIr.loadedFragments)
            val stdlibDirtyFiles = dirtyFiles[stdlibFile] ?: return

            val stdlibSymbolProviders = loadedIr.getSignatureProvidersForLib(stdlibFile)

            val updatedMetadata = KotlinSourceFileMutableMap<DirtyFileMetadata>()
            val idSignatureToFile = updatedMetadata.getExportedSignaturesAndAddMetadata(stdlibSymbolProviders, stdlibFile, stdlibDirtyFiles)

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

        fun buildAndCommitCacheArtifacts(loadedIr: LoadedJsIr): Map<KotlinLibraryFile, IncrementalCacheArtifact> {
            removedIncrementalCaches.forEach {
                if (!it.cacheDir.deleteRecursively()) {
                    icError("can not delete cache directory ${it.cacheDir.absolutePath}")
                }
            }

            val stubbedSignatures = loadedIr.collectSymbolsReplacedWithStubs().mapNotNullTo(hashSetOf()) { it.signature }
            return libraryDependencies.keys.associate { library ->
                val libFile = KotlinLibraryFile(library)
                val incrementalCache = getLibIncrementalCache(libFile)
                val providers = loadedIr.getSignatureProvidersForLib(libFile)
                val signatureToIndexMapping = providers.associate { it.srcFile to it.getSignatureToIndexMapping() }

                val cacheArtifact = incrementalCache.buildAndCommitCacheArtifact(signatureToIndexMapping, stubbedSignatures)

                val libFragment = loadedIr.loadedFragments[libFile] ?: notFoundIcError("loaded fragment", libFile)
                val sourceNames = loadedIr.getIrFileNames(libFragment)
                val sourceFilesFromCache = cacheArtifact.getSourceFiles()
                for (irFile in libFragment.files) {
                    val srcName = sourceNames[irFile] ?: notFoundIcError("source file name", libFile, irFile)
                    if (srcName !in sourceFilesFromCache) {
                        // IC doesn't support cases when extra IrFiles (which don't exist in klib) are added into IrModuleFragment
                        icError("file ${irFile.fileEntry.name} is absent in incremental cache and klib", libFile)
                    }
                }

                libFile to cacheArtifact
            }
        }
    }

    private fun commitCacheAndBuildModuleArtifacts(
        incrementalCacheArtifacts: Map<KotlinLibraryFile, IncrementalCacheArtifact>,
        moduleNames: Map<KotlinLibraryFile, String>,
        rebuiltFileFragments: KotlinSourceFileMap<IrProgramFragments>
    ): List<ModuleArtifact> = stopwatch.measure("Incremental cache - committing artifacts") {
        incrementalCacheArtifacts.map { (libFile, incrementalCacheArtifact) ->
            incrementalCacheArtifact.buildModuleArtifactAndCommitCache(
                moduleName = moduleNames[libFile] ?: notFoundIcError("module name", libFile),
                rebuiltFileFragments = rebuiltFileFragments[libFile] ?: emptyMap(),
                icContext = icContext
            )
        }
    }

    private fun compileDirtyFiles(
        compilerForIC: IrCompilerICInterface,
        loadedIr: LoadedJsIr,
        dirtyFiles: Map<KotlinLibraryFile, Set<KotlinSourceFile>>
    ): MutableList<Triple<KotlinLibraryFile, KotlinSourceFile, () -> IrProgramFragments>> =
        stopwatch.measure("Processing IR - lowering") {
            val dirtyFilesForCompiling = mutableListOf<IrFile>()
            val dirtyFilesForRestoring = mutableListOf<Pair<KotlinLibraryFile, KotlinSourceFile>>()
            for ((libFile, libFragment) in loadedIr.loadedFragments) {
                val dirtySrcFiles = dirtyFiles[libFile] ?: continue
                val sourceNames = loadedIr.getIrFileNames(libFragment)
                for (irFile in libFragment.files) {
                    val srcFile = sourceNames[irFile] ?: notFoundIcError("source file name", libFile, irFile)
                    if (srcFile in dirtySrcFiles) {
                        dirtyFilesForCompiling += irFile
                        dirtyFilesForRestoring += libFile to srcFile
                    }
                }
            }

            val fragmentGenerators = compilerForIC.compile(loadedIr.loadedFragments.values, dirtyFilesForCompiling)

            dirtyFilesForRestoring.mapIndexedTo(ArrayList(dirtyFilesForRestoring.size)) { i, libFileAndSrcFile ->
                Triple(libFileAndSrcFile.first, libFileAndSrcFile.second, fragmentGenerators[i])
            }
        }

    private data class IrForDirtyFilesAndCompiler(
        val incrementalCacheArtifacts: Map<KotlinLibraryFile, IncrementalCacheArtifact>,
        val loadedIr: LoadedJsIr,
        val dirtyFiles: Map<KotlinLibraryFile, Set<KotlinSourceFile>>,
        val irCompiler: IrCompilerICInterface
    )

    private fun loadIrForDirtyFilesAndInitCompiler(): IrForDirtyFilesAndCompiler {
        val updater = CacheUpdaterInternal()

        stopwatch.startNext("Modified files - checking hashes and collecting")
        val modifiedFiles = updater.loadModifiedFiles()

        stopwatch.startNext("Modified files - collecting exported signatures")
        val dirtyFileExports = updater.collectExportedSymbolsForDirtyFiles(modifiedFiles)
        val stubbedSignatures = updater.collectStubbedSignatures()

        stopwatch.startNext("Modified files - loading and linking IR")
        val jsIrLinkerLoader = JsIrLinkerLoader(
            compilerConfiguration = compilerConfiguration,
            dependencyGraph = updater.libraryDependencies,
            mainModuleFriends = updater.mainModuleFriendLibraries,
            irFactory = icContext.createIrFactory(),
            stubbedSignatures = stubbedSignatures
        )
        var loadedIr = jsIrLinkerLoader.loadIr(dirtyFileExports)

        var iterations = 0
        var lastDirtyFiles: KotlinSourceFileMap<KotlinSourceFileExports> = dirtyFileExports

        while (true) {
            stopwatch.startNext("Dependencies ($iterations) - updating a dependency graph")
            val dirtyMetadata = updater.rebuildDirtySourceMetadata(loadedIr, lastDirtyFiles)

            stopwatch.startNext("Dependencies ($iterations) - collecting files with updated exports and imports")
            val filesWithModifiedExportsOrImports = updater.collectFilesWithModifiedExportsAndImports(dirtyMetadata)

            stopwatch.startNext("Dependencies ($iterations) - collecting exported signatures for files with updated exports and imports")
            val filesToRebuild = updater.collectFilesToRebuildSignatures(filesWithModifiedExportsOrImports)
            dirtyFileExports.copyFilesFrom(filesToRebuild)

            stopwatch.startNext("Dependencies ($iterations) - collecting files that contain updated stubbed symbols")
            val filesWithUpdatedStubbedSymbolsToRebuild = updater.collectFilesWithUpdatedStubbedSymbols(dirtyFileExports)
            dirtyFileExports.copyFilesFrom(filesWithUpdatedStubbedSymbolsToRebuild)

            lastDirtyFiles = filesToRebuild.combineWith(filesWithUpdatedStubbedSymbolsToRebuild)

            if (lastDirtyFiles.isEmpty()) {
                break
            }

            stopwatch.startNext("Dependencies ($iterations) - loading and linking IR for files with modified exports and imports")
            loadedIr = jsIrLinkerLoader.loadIr(lastDirtyFiles)
            iterations++
        }

        if (iterations != 0) {
            stopwatch.startNext("Loading and linking all IR")
            loadedIr = jsIrLinkerLoader.loadIr(dirtyFileExports)
        }

        stopwatch.startNext("Processing IR - initializing backend context")
        val mainModuleFragment = loadedIr.loadedFragments[mainLibraryFile] ?: notFoundIcError("main module fragment", mainLibraryFile)
        val compilerForIC = icContext.createCompiler(mainModuleFragment, compilerConfiguration)

        // Load declarations referenced during `context` initialization
        loadedIr.loadUnboundSymbols()

        val dirtyFiles = dirtyFileExports.entries.associateTo(newHashMapWithExpectedSize(dirtyFileExports.size)) {
            it.key to HashSet(it.value.keys)
        }

        stopwatch.startNext("Processing IR - updating intrinsics and builtins dependencies")
        updater.updateStdlibIntrinsicDependencies(loadedIr, mainModuleFragment, dirtyFiles)

        stopwatch.startNext("Incremental cache - building artifacts")
        val incrementalCachesArtifacts = updater.buildAndCommitCacheArtifacts(loadedIr)

        stopwatch.stop()
        return IrForDirtyFilesAndCompiler(incrementalCachesArtifacts, loadedIr, dirtyFiles, compilerForIC)
    }

    private data class FragmentGenerators(
        val incrementalCacheArtifacts: Map<KotlinLibraryFile, IncrementalCacheArtifact>,
        val moduleNames: Map<KotlinLibraryFile, String>,
        val generators: MutableList<Triple<KotlinLibraryFile, KotlinSourceFile, () -> IrProgramFragments>>
    )

    private fun loadIrAndMakeIrFragmentGenerators(): FragmentGenerators {
        val (incrementalCachesArtifacts, loadedIr, dirtyFiles, irCompiler) = loadIrForDirtyFilesAndInitCompiler()

        val moduleNames = loadedIr.loadedFragments.entries.associate { it.key to it.value.name.asString() }

        val rebuiltFragmentGenerators = compileDirtyFiles(irCompiler, loadedIr, dirtyFiles)

        return FragmentGenerators(incrementalCachesArtifacts, moduleNames, rebuiltFragmentGenerators)
    }

    private fun generateIrFragments(
        generators: MutableList<Triple<KotlinLibraryFile, KotlinSourceFile, () -> IrProgramFragments>>
    ): KotlinSourceFileMap<IrProgramFragments> = stopwatch.measure("Processing IR - generating program fragments") {
        val rebuiltFragments = KotlinSourceFileMutableMap<IrProgramFragments>()
        while (generators.isNotEmpty()) {
            val (libFile, srcFile, fragmentGenerator) = generators.removeFirst()
            rebuiltFragments[libFile, srcFile] = fragmentGenerator()
        }
        rebuiltFragments
    }

    /**
     * This method performs the following routine:
     *  - Estimates dirty files that must be relowered;
     *  - Creates a compiler instance by calling [compilerInterfaceFactory];
     *  - Runs the compiler (lowering pipeline) for the dirty files (see [IrCompilerICInterface]);
     *  - Transforms lowered IR to JS AST fragments [IrProgramFragments];
     *  - Saves the cache data on the disk.
     *
     *  @return A module artifact list, where [JsModuleArtifact] represents a compiled klib.
     *   It contains either paths to files with serialized JS AST or the deserialized [IrProgramFragments] objects themselves
     *   for every file in the generating JS module. The list should be used for building the final JS module in [JsExecutableProducer]
     */
    fun actualizeCaches(): List<ModuleArtifact> {
        stopwatch.clear()
        dirtyFileStats.clear()

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
): Pair<IrModuleFragment, List<Pair<IrFile, IrProgramFragments>>> {
    val irInterner = IrInterningService()
    val emptyMetadata = object : KotlinSourceFileExports() {
        override val inverseDependencies = KotlinSourceFileMap<Set<IdSignature>>(emptyMap())
    }

    val libFile = KotlinLibraryFile(library)
    val dirtySrcFiles = dirtyFiles?.let {
        KotlinSourceFile.fromSources(it.toList())
    } ?: KotlinLoadedLibraryHeader(library, irInterner).sourceFileFingerprints.keys

    val modifiedFiles = mapOf(libFile to dirtySrcFiles.associateWith { emptyMetadata })

    val jsIrLoader = JsIrLinkerLoader(configuration, dependencyGraph, emptyList(), irFactory, emptySet())
    val loadedIr = jsIrLoader.loadIr(KotlinSourceFileMap<KotlinSourceFileExports>(modifiedFiles), true)

    val currentIrModule = loadedIr.loadedFragments[libFile] ?: notFoundIcError("loaded fragment", libFile)
    val dirtyIrFiles = dirtyFiles?.let {
        val files = it.toSet()
        currentIrModule.files.memoryOptimizedFilter { irFile -> irFile.fileEntry.name in files }
    } ?: currentIrModule.files

    val compilerWithIC = JsIrCompilerWithIC(
        currentIrModule,
        mainArguments,
        configuration,
        JsGenerationGranularity.PER_MODULE,
        PhaseConfig(getJsPhases(configuration)),
        exportedDeclarations,
    )

    // Load declarations referenced during `context` initialization
    loadedIr.loadUnboundSymbols()
    irInterner.reset()

    val fragments = compilerWithIC.compile(loadedIr.loadedFragments.values, dirtyIrFiles).memoryOptimizedMap { it() }

    return currentIrModule to dirtyIrFiles.zip(fragments)
}
