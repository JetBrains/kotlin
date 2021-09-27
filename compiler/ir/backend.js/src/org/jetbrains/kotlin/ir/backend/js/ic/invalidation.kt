/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.ic

import org.jetbrains.kotlin.backend.common.serialization.IdSignatureDeserializer
import org.jetbrains.kotlin.backend.common.serialization.IrLibraryBytesSource
import org.jetbrains.kotlin.backend.common.serialization.IrLibraryFileFromBytes
import org.jetbrains.kotlin.backend.common.serialization.codedInputStream
import org.jetbrains.kotlin.backend.common.serialization.signature.IdSignatureDescriptor
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.backend.js.JsFactories
import org.jetbrains.kotlin.ir.backend.js.jsResolveLibraries
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.JsIrLinker
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.JsManglerDesc
import org.jetbrains.kotlin.ir.backend.js.moduleName
import org.jetbrains.kotlin.ir.backend.js.toResolverLogger
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.persistent.PersistentIrFactory
import org.jetbrains.kotlin.ir.descriptors.IrBuiltInsOverDescriptors
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.konan.properties.propertyList
import org.jetbrains.kotlin.library.KLIB_PROPERTY_DEPENDS
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.unresolvedDependencies
import org.jetbrains.kotlin.protobuf.ExtensionRegistryLite
import org.jetbrains.kotlin.psi2ir.generators.TypeTranslatorImpl
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import java.io.File
import org.jetbrains.kotlin.backend.common.serialization.proto.IrFile as ProtoFile

private fun KotlinLibrary.fingerprint(fileIndex: Int): Hash {
    return ((((types(fileIndex).md5() * 31) + signatures(fileIndex).md5()) * 31 + strings(fileIndex).md5()) * 31 + declarations(fileIndex).md5()) * 31 + bodies(fileIndex).md5()
}

private fun invalidateCacheForModule(
    library: KotlinLibrary,
    libraryFiles: List<String>,
    externalHashes: Map<IdSignature, TransHash>,
    cachedInlineHashesForFile: MutableMap<String, Map<IdSignature, TransHash>>,
    cacheProvider: PersistentCacheProvider,
    cacheConsumer: PersistentCacheConsumer,
    signatureResolver: (String, Int) -> IdSignature,
    fileFingerPrints: MutableMap<String, Hash>
): Set<String> {

    val dirtyFiles = mutableSetOf<String>()

    for ((index, file) in libraryFiles.withIndex()) {

        // 1. get cached fingerprints
        val fileOldFingerprint = cacheProvider.fileFingerPrint(file)

        // 2. calculate new fingerprints
        val fileNewFingerprint = library.fingerprint(index)

        if (fileOldFingerprint != fileNewFingerprint) {
            fileFingerPrints[file] = fileNewFingerprint
            cachedInlineHashesForFile.remove(file)

            // 3. form initial dirty set
            dirtyFiles.add(file)
        }
    }

    // 4. extend dirty set with inline functions

    val graphCache = mutableMapOf<FilePath, Collection<Pair<IdSignature, TransHash>>>()

    var oldSize: Int
    do {
        oldSize = dirtyFiles.size

        if (dirtyFiles.size == libraryFiles.size) break

        for (file in libraryFiles) {

            if (file in dirtyFiles) continue

            // check for clean file
            val inlineGraph = graphCache.getOrPut(file) { cacheProvider.inlineGraphForFile(file) { signatureResolver(file, it) } }

            for ((sig, oldHash) in inlineGraph) {
                val actualHash = externalHashes[sig] ?: cachedInlineHashesForFile.values.firstNotNullOfOrNull { it[sig] }

                // null means inline function is from dirty file, could be a bit more optimal
                if (actualHash != null) {
                    if (oldHash == actualHash) continue
                }

                cachedInlineHashesForFile.remove(file)
                dirtyFiles.add(file)
                fileFingerPrints[file] = cacheProvider.fileFingerPrint(file)
            }
        }
    } while (oldSize != dirtyFiles.size)

    // 5. invalidate file caches
    for (dirty in dirtyFiles) {
        cacheConsumer.invalidateForFile(dirty)
    }

    return dirtyFiles
}

private fun KotlinLibrary.filesAndSigReaders(): List<Pair<String, IdSignatureDeserializer>> {
    val fileSize = fileCount()
    val result = ArrayList<Pair<String, IdSignatureDeserializer>>(fileSize)
    val extReg = ExtensionRegistryLite.newInstance()

    for (i in 0 until fileSize) {
        val fileStream = file(i).codedInputStream
        val fileProto = ProtoFile.parseFrom(fileStream, extReg)
        val sigReader = IdSignatureDeserializer(IrLibraryFileFromBytes(object : IrLibraryBytesSource() {
            private fun err(): Nothing = error("Not supported")
            override fun irDeclaration(index: Int): ByteArray = err()

            override fun type(index: Int): ByteArray = err()

            override fun signature(index: Int): ByteArray = signature(index, i)

            override fun string(index: Int): ByteArray = string(index, i)

            override fun body(index: Int): ByteArray = err()

            override fun debugInfo(index: Int): ByteArray? = null
        }), null)

        result.add(fileProto.fileEntry.name to sigReader)
    }

    return result
}

private fun buildCacheForModule(
    libraryPath: String,
    irModule: IrModuleFragment,
    dependencies: Collection<IrModuleFragment>,
    dirtyFiles: Collection<String>,
    cleanInlineHashes: Map<IdSignature, Hash>,
    cacheConsumer: PersistentCacheConsumer,
    signatureDeserializers: Map<FilePath, Map<IdSignature, Int>>,
    fileFingerPrints: Map<String, Hash>
) {
    val dirtyIrFiles = irModule.files.filter { it.fileEntry.name in dirtyFiles }

    val flatHasher = InlineFunctionFlatHashBuilder()

    dirtyIrFiles.forEach { it.acceptVoid(flatHasher) }

    val flatHashes = flatHasher.idToHashMap

    val hashProvider = object : InlineFunctionHashProvider {
        override fun hashForExternalFunction(declaration: IrSimpleFunction): TransHash? {
            return declaration.symbol.signature?.let { cleanInlineHashes[it] }
        }
    }

    val hashBuilder = InlineFunctionHashBuilder(hashProvider, flatHashes)

    val hashes = hashBuilder.buildHashes(dirtyIrFiles)

    val splitPerFiles =
        hashes.entries.filter { !it.key.isFakeOverride && (it.key.symbol.signature?.visibleCrossFile ?: false) }.groupBy({ it.key.file }) {
            val signature = it.key.symbol.signature ?: error("Unexpected private inline fun ${it.key.render()}")
            signature to it.value
        }

    val inlineGraph = hashBuilder.buildInlineGraph(hashes)

    dirtyIrFiles.forEach { irFile ->
        val fileName = irFile.fileEntry.name
        val sigToIndexMap = signatureDeserializers[fileName] ?: error("No sig2id mapping found for $fileName")
        val indexResolver: (IdSignature) -> Int = { sigToIndexMap[it] ?: error("No index found for sig $it") }
        val inlineHashes = splitPerFiles[irFile] ?: emptyList()
        cacheConsumer.commitInlineFunctions(fileName, inlineHashes, indexResolver)
        val fileInlineGraph = inlineGraph[irFile] ?: emptyList()
        cacheConsumer.commitInlineGraph(fileName, fileInlineGraph, indexResolver)
        cacheConsumer.commitFileFingerPrint(fileName, fileFingerPrints[fileName] ?: error("No fingerprint found for file $fileName"))
    }

    // TODO: actual way of building a cache could change in future
    buildCacheForModuleFiles(irModule, dependencies, dirtyFiles, cacheConsumer)

    cacheConsumer.commitLibraryPath(libraryPath)
}

private fun loadModules(
    languageVersionSettings: LanguageVersionSettings,
    dependencyGraph: Map<KotlinLibrary, Collection<KotlinLibrary>>
): Map<ModuleDescriptor, KotlinLibrary> {
    val descriptors = mutableMapOf<KotlinLibrary, ModuleDescriptorImpl>()

    var runtimeModule: ModuleDescriptorImpl? = null

    // TODO: deduplicate this code using part from klib.kt
    fun getModuleDescriptor(current: KotlinLibrary): ModuleDescriptorImpl = descriptors.getOrPut(current) {
        val isBuiltIns = current.unresolvedDependencies.isEmpty()

        val lookupTracker = LookupTracker.DO_NOTHING
        val md = JsFactories.DefaultDeserializedDescriptorFactory.createDescriptorOptionalBuiltIns(
            current,
            languageVersionSettings,
            LockBasedStorageManager.NO_LOCKS,
            runtimeModule?.builtIns,
            packageAccessHandler = null, // TODO: This is a speed optimization used by Native. Don't bother for now.
            lookupTracker = lookupTracker
        )
        if (isBuiltIns) runtimeModule = md

        val dependencies = dependencyGraph[current]!!.map { getModuleDescriptor(it) }
        md.setDependencies(listOf(md) + dependencies)
        md
    }


    return dependencyGraph.keys.associateBy { klib -> getModuleDescriptor(klib) }
}

@OptIn(ObsoleteDescriptorBasedAPI::class)
private fun createLinker(configuration: CompilerConfiguration, loadedModules: Map<ModuleDescriptor, KotlinLibrary>): JsIrLinker {
    val logger = configuration[IrMessageLogger.IR_MESSAGE_LOGGER] ?: IrMessageLogger.None
    val signaturer = IdSignatureDescriptor(JsManglerDesc)
    val symbolTable = SymbolTable(signaturer, PersistentIrFactory())
    val moduleDescriptor = loadedModules.keys.last()
    val typeTranslator = TypeTranslatorImpl(symbolTable, configuration.languageVersionSettings, moduleDescriptor)
    val irBuiltIns = IrBuiltInsOverDescriptors(moduleDescriptor.builtIns, typeTranslator, symbolTable)
    return JsIrLinker(null, logger, irBuiltIns, symbolTable, null, null)
}


fun Map<KotlinLibrary, Collection<KotlinLibrary>>.transitiveClosure(library: KotlinLibrary): Collection<KotlinLibrary> {
    val visited = mutableSetOf<KotlinLibrary>()

    fun walk(lib: KotlinLibrary) {
        if (visited.add(lib)) {
            get(lib)?.let { it.forEach { d -> walk(d) } }
        }
    }

    walk(library)

    return visited
}

private fun createCacheProvider(path: String): PersistentCacheProvider {
    return PersistentCacheProviderImpl(path)
}

private fun createCacheConsumer(path: String): PersistentCacheConsumer {
    return PersistentCacheConsumerImpl(path)
}

private fun loadCacheInfo(cachePaths: Collection<String>): MutableMap<ModulePath, String> {
    val caches = cachePaths.map { CacheInfo.load(it) ?: error("Cannot load IC cache from $it") }
    val result = mutableMapOf<ModulePath, String>()
    return caches.associateTo(result) { it.libPath.toCanonicalPath() to it.path }
}

private fun loadLibraries(configuration: CompilerConfiguration, dependencies: Collection<String>): Map<ModulePath, KotlinLibrary> {
    val allResolvedDependencies = jsResolveLibraries(
        dependencies,
        configuration[JSConfigurationKeys.REPOSITORIES] ?: emptyList(),
        configuration[IrMessageLogger.IR_MESSAGE_LOGGER].toResolverLogger()
    )

    return allResolvedDependencies.getFullList().associateBy { it.libraryFile.path.toCanonicalPath() }
}

private fun String.toCanonicalPath(): String = File(this).canonicalPath

typealias ModuleName = String
typealias ModulePath = String
typealias FilePath = String

fun actualizeCacheForModule(
    moduleName: String,
    cachePath: String,
    compilerConfiguration: CompilerConfiguration,
    dependencies: Collection<ModulePath>,
    icCachePaths: Collection<String>
): Boolean {
    val icCacheMap: Map<ModulePath, String> = loadCacheInfo(icCachePaths).also {
        it[moduleName.toCanonicalPath()] = cachePath
    }

    val libraries: Map<ModulePath, KotlinLibrary> = loadLibraries(compilerConfiguration, dependencies)

    val persistentCacheProviders = icCacheMap.map { (lib, cache) ->
        libraries[lib.toCanonicalPath()]!! to createCacheProvider(cache)
    }.toMap()

    val nameToKotlinLibrary: Map<ModuleName, KotlinLibrary> = libraries.values.associateBy { it.moduleName }

    val dependencyGraph = libraries.values.associateWith {
        it.manifestProperties.propertyList(KLIB_PROPERTY_DEPENDS, escapeInQuotes = true).map { depName ->
            nameToKotlinLibrary[depName] ?: error("No Library found for $depName")
        }
    }

    val currentModule = libraries[moduleName.toCanonicalPath()] ?: error("No loaded library found for path $moduleName")
    val persistentCacheConsumer = createCacheConsumer(cachePath)

    return actualizeCacheForModule(currentModule, compilerConfiguration, dependencyGraph, persistentCacheProviders, persistentCacheConsumer)
}


private fun actualizeCacheForModule(
    library: KotlinLibrary,
    configuration: CompilerConfiguration,
    dependencyGraph: Map<KotlinLibrary, Collection<KotlinLibrary>>,
    persistentCacheProviders: Map<KotlinLibrary, PersistentCacheProvider>,
    persistentCacheConsumer: PersistentCacheConsumer
): Boolean {
    // 1. Invalidate
    val dependencies = dependencyGraph[library]!!

    val filesAndSigReaders = library.filesAndSigReaders()
    val signatureDeserializers = filesAndSigReaders.toMap()
    val libraryFiles = filesAndSigReaders.map { it.first }

    val depReaders = dependencies.associateWith { it.filesAndSigReaders().toMap() }

    val signatureResolver: (String, Int) -> IdSignature = { f, s ->
        signatureDeserializers[f]?.deserializeIdSignature(s) ?: error("Cannot deserialize sig $s from $f")
    }

    val sigHashes = mutableMapOf<IdSignature, TransHash>()
    dependencies.forEach { lib ->
        persistentCacheProviders[lib]?.let { provider ->
            val moduleReaders = depReaders[lib]!!
            val inlineHashes = provider.allInlineHashes { f, i ->
                moduleReaders[f]!!.deserializeIdSignature(i)
            }
            sigHashes.putAll(inlineHashes)
        }
    }

    val fileFingerPrints = mutableMapOf<String, Hash>()

    val currentLibraryCacheProvider = persistentCacheProviders[library] ?: error("No cache provider for $library")

    val fileCachedInlineHashes = mutableMapOf<String, Map<IdSignature, TransHash>>()

    filesAndSigReaders.forEach { (filePath, sigReader) ->
        fileCachedInlineHashes[filePath] =
            currentLibraryCacheProvider.inlineHashes(filePath) { s -> sigReader.deserializeIdSignature(s) }
    }

    val dirtySet = invalidateCacheForModule(
        library,
        libraryFiles,
        sigHashes,
        fileCachedInlineHashes,
        currentLibraryCacheProvider,
        persistentCacheConsumer,
        signatureResolver,
        fileFingerPrints
    )

    if (dirtySet.isEmpty()) return true // up-to-date

    // 2. Build

    val loadedModules = loadModules(configuration.languageVersionSettings, dependencyGraph)

    val jsIrLinker = createLinker(configuration, loadedModules)

    val irModules = ArrayList<Pair<IrModuleFragment, KotlinLibrary>>(loadedModules.size)

    // TODO: modules deserialized here have to be reused for cache building further
    for ((descriptor, loadedLibrary) in loadedModules) {
        if (library == loadedLibrary) {
            irModules.add(jsIrLinker.deserializeDirtyFiles(descriptor, loadedLibrary, dirtySet) to loadedLibrary)
        } else {
            irModules.add(jsIrLinker.deserializeHeadersWithInlineBodies(descriptor, loadedLibrary) to loadedLibrary)
        }
    }

    jsIrLinker.init(null, emptyList())

    ExternalDependenciesGenerator(jsIrLinker.symbolTable, listOf(jsIrLinker)).generateUnboundSymbolsAsDependencies()

    jsIrLinker.postProcess()

    val currentIrModule = irModules.find { it.second == library }?.first!!
    val currentModuleDeserializer = jsIrLinker.moduleDeserializer(currentIrModule.descriptor)

    for (file in libraryFiles) {
        if (file !in dirtySet) {
            val sigDeserializer = signatureDeserializers[file]!!
            sigHashes.putAll(currentLibraryCacheProvider.inlineHashes(file) { sigDeserializer.deserializeIdSignature(it) })
        }
    }

    val deserializers = dirtySet.associateWith { currentModuleDeserializer.signatureDeserializerForFile(it).signatureToIndexMapping() }

    buildCacheForModule(
        library.libraryFile.canonicalPath,
        currentIrModule,
        irModules.map { it.first },
        dirtySet,
        sigHashes,
        persistentCacheConsumer,
        deserializers,
        fileFingerPrints
    )
    return false // invalidated and re-built
}

fun rebuildCacheForDirtyFiles(
    library: KotlinLibrary,
    configuration: CompilerConfiguration,
    dependencyGraph: Map<KotlinLibrary, Collection<KotlinLibrary>>,
    dirtyFiles: Collection<String>?,
    cacheConsumer: PersistentCacheConsumer
) {
    val loadedModules = loadModules(configuration.languageVersionSettings, dependencyGraph)

    val jsIrLinker = createLinker(configuration, loadedModules)

    val irModules = ArrayList<Pair<IrModuleFragment, KotlinLibrary>>(loadedModules.size)

    // TODO: modules deserialized here have to be reused for cache building further
    for ((descriptor, loadedLibrary) in loadedModules) {
        if (library == loadedLibrary) {
            if (dirtyFiles != null) {
                irModules.add(jsIrLinker.deserializeDirtyFiles(descriptor, loadedLibrary, dirtyFiles) to loadedLibrary)
            } else {
                irModules.add(jsIrLinker.deserializeFullModule(descriptor, loadedLibrary) to loadedLibrary)
            }
        } else {
            irModules.add(jsIrLinker.deserializeHeadersWithInlineBodies(descriptor, loadedLibrary) to loadedLibrary)
        }
    }

    jsIrLinker.init(null, emptyList())

    ExternalDependenciesGenerator(jsIrLinker.symbolTable, listOf(jsIrLinker)).generateUnboundSymbolsAsDependencies()

    jsIrLinker.postProcess()

    val currentIrModule = irModules.find { it.second == library }?.first!!

    buildCacheForModuleFiles(currentIrModule, irModules.map { it.first }, dirtyFiles, cacheConsumer)
}

@Suppress("UNUSED_PARAMETER")
private fun buildCacheForModuleFiles(
    currentModule: IrModuleFragment,
    dependencies: Collection<IrModuleFragment>,
    dirtyFiles: Collection<String>?, // if null consider the whole module dirty
    cacheConsumer: PersistentCacheConsumer
) {
//    println("creating caches for module ${currentModule.name}")
//    println("Store them into $cacheConsumer")
//    val dirtyS = if (dirtyFiles == null) "[ALL]" else dirtyFiles.joinToString(",", "[", "]") { it }
//    println("Dirty files -> $dirtyS")
}
