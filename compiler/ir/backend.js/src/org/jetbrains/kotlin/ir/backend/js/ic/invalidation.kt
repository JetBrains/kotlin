/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.ic

import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.ir.backend.js.*
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.JsIrLinker
import org.jetbrains.kotlin.ir.declarations.IrFactory
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.konan.properties.propertyList
import org.jetbrains.kotlin.library.KLIB_PROPERTY_DEPENDS
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.protobuf.ExtensionRegistryLite
import org.jetbrains.kotlin.psi2ir.descriptors.IrBuiltInsOverDescriptors
import org.jetbrains.kotlin.psi2ir.generators.TypeTranslatorImpl
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import java.io.File


fun interface CacheExecutor {
    fun execute(
        currentModule: IrModuleFragment,
        dependencies: Collection<IrModuleFragment>,
        deserializer: JsIrLinker,
        configuration: CompilerConfiguration,
        dirtyFiles: Collection<String>?, // if null consider the whole module dirty
        artifactCache: ArtifactCache,
        exportedDeclarations: Set<FqName>,
        mainArguments: List<String>?,
    )
}

sealed class CacheUpdateStatus {
    object FastPath : CacheUpdateStatus()
    class NoDirtyFiles(val removed: Set<String>) : CacheUpdateStatus()
    class Dirty(val removed: Set<String>, val updated: Set<String>, val updatedAll: Boolean) : CacheUpdateStatus()
}

class CacheUpdater(
    private val rootModule: String,
    private val allModules: Collection<String>,
    private val compilerConfiguration: CompilerConfiguration,
    private val icCachePaths: Collection<String>,
    private val irFactory: () -> IrFactory,
    private val mainArguments: List<String>?,
    private val executor: CacheExecutor
) {
    private fun KotlinLibrary.moduleCanonicalName() = libraryFile.canonicalPath

    private inner class KLibCacheUpdater(
        private val library: KotlinLibrary,
        private val dependencyGraph: Map<KotlinLibrary, List<KotlinLibrary>>,
        private val incrementalCache: IncrementalCache,
        private val klibIncrementalCaches: Map<KotlinLibrary, IncrementalCache>
    ) {
        private fun invalidateCacheForModule(externalHashes: Map<IdSignature, ICHash>): Pair<Set<String>, Map<String, ICHash>> {
            val fileFingerPrints = mutableMapOf<String, ICHash>()
            val dirtyFiles = mutableSetOf<String>()

            if (incrementalCache.klibUpdated) {
                for ((index, file) in incrementalCache.srcFilesInOrderFromKLib.withIndex()) {
                    // 1. get cached fingerprints
                    val fileOldFingerprint = incrementalCache.srcFingerprints[file] ?: 0

                    // 2. calculate new fingerprints
                    val fileNewFingerprint = library.fingerprint(index)

                    if (fileOldFingerprint != fileNewFingerprint) {
                        fileFingerPrints[file] = fileNewFingerprint
                        incrementalCache.invalidateForSrcFile(file)

                        // 3. form initial dirty set
                        dirtyFiles.add(file)
                    }
                }
            }

            // 4. extend dirty set with inline functions
            do {
                if (dirtyFiles.size == incrementalCache.srcFilesInOrderFromKLib.size) break

                val oldSize = dirtyFiles.size
                for (file in incrementalCache.srcFilesInOrderFromKLib) {

                    if (file in dirtyFiles) continue

                    // check for clean file
                    val usedInlineFunctions = incrementalCache.usedFunctions[file] ?: emptyMap()

                    for ((sig, oldHash) in usedInlineFunctions) {
                        val actualHash = externalHashes[sig] ?: incrementalCache.implementedFunctions.firstNotNullOfOrNull { it[sig] }
                        // null means inline function is from dirty file, could be a bit more optimal
                        if (actualHash == null || oldHash != actualHash) {
                            fileFingerPrints[file] = incrementalCache.srcFingerprints[file] ?: error("Cannot find fingerprint for $file")
                            incrementalCache.invalidateForSrcFile(file)
                            dirtyFiles.add(file)
                            break
                        }
                    }
                }
            } while (oldSize != dirtyFiles.size)

            // 5. invalidate file caches
            for (deleted in incrementalCache.deletedSrcFiles) {
                incrementalCache.invalidateForSrcFile(deleted)
            }

            return dirtyFiles to fileFingerPrints
        }

        private fun getDependencySubGraph(): Map<KotlinLibrary, List<KotlinLibrary>> {
            val subGraph = mutableMapOf<KotlinLibrary, List<KotlinLibrary>>()

            fun addDependsFor(library: KotlinLibrary) {
                if (library in subGraph) {
                    return
                }
                val dependencies = dependencyGraph[library] ?: error("Cannot find dependencies for ${library.libraryName}")
                subGraph[library] = dependencies
                for (dependency in dependencies) {
                    addDependsFor(dependency)
                }
            }
            addDependsFor(library)
            return subGraph
        }

        private fun buildCacheForModule(
            irModule: IrModuleFragment,
            deserializer: JsIrLinker,
            dependencies: Collection<IrModuleFragment>,
            dirtyFiles: Collection<String>,
            cleanInlineHashes: Map<IdSignature, ICHash>,
            fileFingerPrints: Map<String, ICHash>
        ) {
            val dirtyIrFiles = irModule.files.filter { it.fileEntry.name in dirtyFiles }

            val flatHashes = InlineFunctionFlatHashBuilder().apply {
                dirtyIrFiles.forEach { it.acceptVoid(this) }
            }.getFlatHashes()

            val hashProvider = object : InlineFunctionHashProvider {
                override fun hashForExternalFunction(declaration: IrFunction): ICHash? {
                    return declaration.symbol.signature?.let { cleanInlineHashes[it] }
                }
            }

            val hashBuilder = InlineFunctionHashBuilder(hashProvider, flatHashes)

            val hashes = hashBuilder.buildHashes(dirtyIrFiles)

            val splitPerFiles = hashes.entries.filter { !it.key.isFakeOverride && (it.key.symbol.signature?.visibleCrossFile ?: false) }
                .groupBy({ it.key.file }) {
                    val signature = it.key.symbol.signature ?: error("Unexpected private inline fun ${it.key.render()}")
                    signature to it.value
                }

            val inlineGraph = hashBuilder.buildInlineGraph(hashes)

            dirtyIrFiles.forEach { irFile ->
                val fileName = irFile.fileEntry.name
                incrementalCache.updateHashes(
                    srcPath = fileName,
                    fingerprint = fileFingerPrints[fileName] ?: error("No fingerprint found for file $fileName"),
                    usedFunctions = inlineGraph[irFile],
                    implementedFunctions = splitPerFiles[irFile]?.toMap()
                )
            }

            // TODO: actual way of building a cache could change in future
            executor.execute(
                irModule, dependencies, deserializer, compilerConfiguration, dirtyFiles, incrementalCache, emptySet(), mainArguments
            )
        }

        fun checkLibrariesHash(): Boolean {
            val flatHash = File(library.moduleCanonicalName()).fileHashForIC()
            val dependencies = dependencyGraph[library] ?: error("Cannot find dependencies for ${library.libraryName}")

            var transHash = flatHash
            for (dep in dependencies) {
                val depCache = klibIncrementalCaches[dep] ?: error("Cannot cache info for ${dep.libraryName}")
                transHash = transHash.combineWith(depCache.klibTransitiveHash)
            }
            return incrementalCache.checkAndUpdateCacheFastInfo(flatHash, transHash)
        }

        fun actualizeCacheForModule(): CacheUpdateStatus {
            // 1. Invalidate
            val dependencies = dependencyGraph[library]!!

            val incrementalCache = klibIncrementalCaches[library] ?: error("No cache provider for $library")

            val sigHashes = mutableMapOf<IdSignature, ICHash>()
            dependencies.forEach { lib ->
                klibIncrementalCaches[lib]?.let { libCache ->
                    libCache.fetchCacheDataForDependency()
                    libCache.implementedFunctions.forEach { sigHashes.putAll(it) }
                }
            }

            incrementalCache.fetchFullCacheData()
            val (dirtySet, fileFingerPrints) = invalidateCacheForModule(sigHashes)
            val removed = incrementalCache.deletedSrcFiles

            if (dirtySet.isEmpty()) {
                // up-to-date
                incrementalCache.commitCacheForRemovedSrcFiles()
                return CacheUpdateStatus.NoDirtyFiles(removed)
            }

            // 2. Build
            val jsIrLinkerProcessor = JsIrLinkerLoader(compilerConfiguration, library, getDependencySubGraph(), irFactory())
            val (jsIrLinker, currentIrModule, irModules) = jsIrLinkerProcessor.processJsIrLinker(dirtySet)

            val currentModuleDeserializer = jsIrLinker.moduleDeserializer(currentIrModule.descriptor)

            incrementalCache.implementedFunctions.forEach { sigHashes.putAll(it) }

            for (dirtySrcFile in dirtySet) {
                val signatureMapping = currentModuleDeserializer.signatureDeserializerForFile(dirtySrcFile).signatureToIndexMapping()
                incrementalCache.updateSignatureToIdMapping(dirtySrcFile, signatureMapping)
            }

            buildCacheForModule(currentIrModule, jsIrLinker, irModules, dirtySet, sigHashes, fileFingerPrints)

            val updatedAll = dirtySet.size == incrementalCache.srcFilesInOrderFromKLib.size
            incrementalCache.commitCacheForRebuiltSrcFiles(currentIrModule.name.asString())
            // invalidated and re-built
            return CacheUpdateStatus.Dirty(removed, dirtySet, updatedAll)
        }
    }

    private fun loadLibraries(): Map<String, KotlinLibrary> {
        val allResolvedDependencies = jsResolveLibraries(
            allModules,
            compilerConfiguration[JSConfigurationKeys.REPOSITORIES] ?: emptyList(),
            compilerConfiguration[IrMessageLogger.IR_MESSAGE_LOGGER].toResolverLogger()
        )

        return allResolvedDependencies.getFullList().associateBy { it.moduleCanonicalName() }
    }

    private fun buildDependenciesGraph(libraries: Map<String, KotlinLibrary>): Map<KotlinLibrary, List<KotlinLibrary>> {
        val nameToKotlinLibrary: Map<String, KotlinLibrary> = libraries.values.associateBy { it.moduleName }
        return libraries.values.associateWith {
            it.manifestProperties.propertyList(KLIB_PROPERTY_DEPENDS, escapeInQuotes = true).map { depName ->
                nameToKotlinLibrary[depName] ?: error("No Library found for $depName")
            }
        }
    }

    fun actualizeCaches(callback: (CacheUpdateStatus, String) -> Unit): List<ModuleArtifact> {
        val libraries = loadLibraries()
        val dependencyGraph = buildDependenciesGraph(libraries)
        val configHash = compilerConfiguration.configHashForIC()

        val cacheMap = libraries.values.zip(icCachePaths).toMap()

        val klibIncrementalCaches = mutableMapOf<KotlinLibrary, IncrementalCache>()

        val visitedLibraries = mutableSetOf<KotlinLibrary>()
        fun visitDependency(library: KotlinLibrary) {
            if (library in visitedLibraries) return
            visitedLibraries.add(library)

            val libraryDeps = dependencyGraph[library] ?: error("Unknown library ${library.libraryName}")
            libraryDeps.forEach { visitDependency(it) }

            val cachePath = cacheMap[library] ?: error("Unknown cache for library ${library.libraryName}")
            val incrementalCache = IncrementalCache(library, cachePath)
            klibIncrementalCaches[library] = incrementalCache

            incrementalCache.invalidateCacheForNewConfig(configHash)
            val cacheUpdater = KLibCacheUpdater(library, dependencyGraph, incrementalCache, klibIncrementalCaches)
            val updateStatus = when {
                cacheUpdater.checkLibrariesHash() -> CacheUpdateStatus.FastPath
                else -> cacheUpdater.actualizeCacheForModule()
            }
            callback(updateStatus, library.libraryFile.path)
        }

        val rootModuleCanonical = File(rootModule).canonicalPath
        val mainLibrary = libraries[rootModuleCanonical] ?: error("Main library not found in libraries: $rootModuleCanonical")
        visitDependency(mainLibrary)
        return klibIncrementalCaches.map { it.value.fetchArtifacts() }
    }
}


// Used for tests only
fun rebuildCacheForDirtyFiles(
    library: KotlinLibrary,
    configuration: CompilerConfiguration,
    dependencyGraph: Map<KotlinLibrary, List<KotlinLibrary>>,
    dirtyFiles: Collection<String>?,
    artifactCache: ArtifactCache,
    irFactory: IrFactory,
    exportedDeclarations: Set<FqName>,
    mainArguments: List<String>?,
): String {
    val jsIrLinkerProcessor = JsIrLinkerLoader(configuration, library, dependencyGraph, irFactory)
    val (jsIrLinker, currentIrModule, irModules) = jsIrLinkerProcessor.processJsIrLinker(dirtyFiles)

    buildCacheForModuleFiles(
        currentIrModule,
        irModules,
        jsIrLinker,
        configuration,
        dirtyFiles,
        artifactCache,
        exportedDeclarations,
        mainArguments
    )
    return currentIrModule.name.asString()
}

@Suppress("UNUSED_PARAMETER")
fun buildCacheForModuleFiles(
    currentModule: IrModuleFragment,
    dependencies: Collection<IrModuleFragment>,
    deserializer: JsIrLinker,
    configuration: CompilerConfiguration,
    dirtyFiles: Collection<String>?, // if null consider the whole module dirty
    artifactCache: ArtifactCache,
    exportedDeclarations: Set<FqName>,
    mainArguments: List<String>?,
) {
    compileWithIC(
        currentModule,
        configuration = configuration,
        deserializer = deserializer,
        dependencies = dependencies,
        mainArguments = mainArguments,
        exportedDeclarations = exportedDeclarations,
        filesToLower = dirtyFiles?.toSet(),
        artifactCache = artifactCache,
    )
}
