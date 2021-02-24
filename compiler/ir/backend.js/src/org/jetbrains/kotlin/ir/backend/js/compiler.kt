/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("UNUSED_VARIABLE", "UNUSED_PARAMETER")

package org.jetbrains.kotlin.ir.backend.js

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analyzer.AbstractAnalyzerWithCompilerReport
import org.jetbrains.kotlin.backend.common.lower.inline.InlineFunctionFlatHashBuilder
import org.jetbrains.kotlin.backend.common.lower.inline.InlineFunctionHashBuilder
import org.jetbrains.kotlin.backend.common.lower.inline.InlineFunctionHashProvider
import org.jetbrains.kotlin.backend.common.lower.inline.md5
import org.jetbrains.kotlin.backend.common.phaser.PhaseConfig
import org.jetbrains.kotlin.backend.common.phaser.invokeToplevel
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.ir.backend.js.lower.generateTests
import org.jetbrains.kotlin.ir.backend.js.lower.moveBodilessDeclarationsToSeparatePlace
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.JsIrLinker
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.IrModuleToJsTransformer
import org.jetbrains.kotlin.ir.backend.js.utils.NameTables
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.StageController
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.declarations.persistent.PersistentIrFactory
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.SerializedIrFile
import org.jetbrains.kotlin.library.resolver.KotlinLibraryResolveResult
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.utils.addToStdlib.firstNotNullResult

class CompilerResult(
    val jsCode: JsCode?,
    val dceJsCode: JsCode?,
    val tsDefinitions: String? = null
)

class JsCode(val mainModule: String, val dependencies: Iterable<Pair<String, String>> = emptyList())

typealias Hash = Long

interface PersistentCacheProvider {
    fun invalidateForFile(path: String)

    fun updateFingerPrint(path: String, fingerprint: Long)

    fun md5ForFile(path: String): Long

    fun serializedParts(path: String): SerializedIrFile

    fun inlineGraphForFile(path: String): Collection<Pair<IdSignature, Hash>>

    fun inlineHashes(path: String): Map<IdSignature, Hash>

    fun inlineHashes(): Map<IdSignature, Hash>


    companion object {
        object Empty : PersistentCacheProvider {
            override fun invalidateForFile(path: String) {}
            override fun updateFingerPrint(path: String, fingerprint: Long) {}
            override fun md5ForFile(path: String): Long = 42L
            override fun serializedParts(path: String): SerializedIrFile {
                val zero = ByteArray(0)
                return SerializedIrFile(zero, "", path, zero, zero, zero, zero, zero)
            }

            override fun inlineGraphForFile(path: String): Collection<Pair<IdSignature, Hash>> = emptyList()
            override fun inlineHashes(): Map<IdSignature, Hash> = emptyMap()
            override fun inlineHashes(path: String): Map<IdSignature, Hash> = emptyMap()
        }
    }
}

fun PersistentCacheConsumer.buildForFile(file: IrFile) {
    TODO("Build cache for ${file.fileEntry.name}")
}


fun SerializedIrFile.fingerprint(): Long {
    return ((((types.md5() * 31) + signatures.md5()) * 31 + strings.md5()) * 31 + declarations.md5()) * 31 + bodies.md5()
}


fun <T> Iterable<T>.isIntersectedWith(other: Set<T>): Boolean = any { it in other }

//fun rebuildCache(
//    moduleFragment: IrModuleFragment,
//    cache: PersistentCacheProvider,
//    inlineGraphTracker: InlineGraphTracker,
//    module2changedFiles: Map<IrModuleFragment, Set<String>>
//): Set<String> {
//    // TODO: How to detect deleted files?
//
//    val dirtyFiles = mutableSetOf<String>()
//
//    val currentModulePaths = moduleFragment.files.map { it.fileEntry.name }
//
//    for (file in currentModulePaths) {
//
//        // 1. get cached fingerprints
//        val fileOldFingerprint = cache.md5ForFile(file)
//
//        // 2. calculate new fingerprints
//        val fileNewFingerprint = cache.serializedParts(file).fingerprint()
//
//        if (fileOldFingerprint != fileNewFingerprint) {
//            cache.updateFingerPrint(file, fileNewFingerprint)
//
//            // 3. form initial dirty set
//            dirtyFiles.add(file)
//        }
//    }
//
//    // TODO: rethink using fingerprint of inlined files
//
//    // TODO: increase granularity level from file to inline function
//
//    // 4. Take into account changes from dependencies
//    for (file in currentModulePaths) {
//        val inlineSet = inlineGraphTracker[file]
//
//        if (module2changedFiles.any { it.value.isIntersectedWith(inlineSet) }) {
//            dirtyFiles.add(file)
//        }
//    }
//
//    var dirtySetSize: Int
//
//    // 5. expand dirty inline functions effect across the module
//    do {
//        dirtySetSize = dirtyFiles.size
//
//        for (file in currentModulePaths) {
//            val inlineSet = inlineGraphTracker[file]
//            if (dirtyFiles.isIntersectedWith(inlineSet)) {
//                dirtyFiles.add(file)
//            }
//        }
//
//    } while (dirtySetSize != dirtyFiles.size)
//
//    // 6. invalidate caches
//    for (dirty in dirtyFiles) {
//        cache.invalidateForFile(dirty) // persistent cache
//        inlineGraphTracker.invalidateForFile(dirty) // inline graph
//    }
//
//    // 7. rebuild persistent caches
//    for (dirty in dirtyFiles) {
//        val irFile = moduleFragment.files.single { it.fileEntry.name == dirty }
//        cache.buildForFile(irFile)
//    }
//
//    return dirtyFiles
//}


private fun invalidateCacheForModule(
    libraryFiles: Set<String>,
    externalHashes: Map<IdSignature, Hash>,
    thisHashes: MutableMap<String, Map<IdSignature, Hash>>,
    cacheProvider: PersistentCacheProvider,
    cacheConsumer: PersistentCacheConsumer
): Set<String> {

    val dirtyFiles = mutableSetOf<String>()

    for (file in libraryFiles) {

        // 1. get cached fingerprints
        val fileOldFingerprint = cacheProvider.md5ForFile(file)

        // 2. calculate new fingerprints
        val fileNewFingerprint = cacheProvider.serializedParts(file).fingerprint()

        if (fileOldFingerprint != fileNewFingerprint) {
            cacheConsumer.commitFingerPrint(file, fileNewFingerprint)
            thisHashes.remove(file)

            // 3. form initial dirty set
            dirtyFiles.add(file)
        }
    }

    // 4. extend dirty set with inline functions

    // TODO: should it be met somehow?

    var oldSize: Int
    do {
        oldSize = dirtyFiles.size
        for (file in libraryFiles) {

            if (file in dirtyFiles) continue

            val inlineGraph = cacheProvider.inlineGraphForFile(file)

            for ((sig, oldHash) in inlineGraph) {
                val actualHash = externalHashes[sig] ?: thisHashes.values.firstNotNullResult { it[sig] }

                if (actualHash != null) {
                    if (oldHash == actualHash) continue
                }

                thisHashes.remove(file)
                dirtyFiles.add(file)
            }
        }
    } while (oldSize != dirtyFiles.size)

    // 5. invalidate caches
    for (dirty in dirtyFiles) {
        cacheConsumer.invalidateForFile(dirty)
    }

    return dirtyFiles
}

fun KotlinLibrary.files(): Set<String> {
    TODO("...")
}

interface PersistentCacheConsumer {
    fun commitInlineFunctions(path: String, hashes: Collection<Pair<IdSignature, Hash>>)
    fun commitFingerPrint(path: String, fingerprint: Long)
    fun invalidateForFile(path: String)
}

private fun buildCacheForModule(
    irModule: IrModuleFragment,
    dirtyFiles: Set<String>,
    cleanInlineHashes: Map<IdSignature, Hash>,
    cacheConsumer: PersistentCacheConsumer
) {
    val dirtyIrFiles = irModule.files.filter { it.fileEntry.name in dirtyFiles }

    val flatHasher = InlineFunctionFlatHashBuilder()

    dirtyIrFiles.forEach { it.acceptVoid(flatHasher) }

    val flatHashes = flatHasher.idToHashMap

    val hashProvider = object : InlineFunctionHashProvider {
        override fun hashForExternalFunction(declaration: IrSimpleFunction): Hash? {
            return declaration.symbol.signature?.let { cleanInlineHashes[it] }
        }
    }

    val hashBuilder = InlineFunctionHashBuilder(hashProvider, flatHashes)

    val hashes = hashBuilder.buildHashes(dirtyIrFiles)

    val splitPerFiles = hashes.entries.groupBy({ it.key.file }) {
        val signature = it.key.symbol.signature ?: error("Unexpected private inline fun ${it.key.render()}")
        signature to it.value
    }

    for (irFile in dirtyIrFiles) {
        cacheConsumer.commitInlineFunctions(irFile.fileEntry.name, splitPerFiles[irFile]!!)
        cacheConsumer.buildForFile(irFile)
    }
}

private fun loadModules(rpo: List<KotlinLibrary>, dependencyGraph: Map<KotlinLibrary, Collection<KotlinLibrary>>): Map<ModuleDescriptor, KotlinLibrary> {
    TODO("...")
}

private fun createLinker(loadedModules: Map<ModuleDescriptor, KotlinLibrary>): JsIrLinker {
    TODO("....")
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

fun actualizationCacheLoop(
    modulesRpo: List<KotlinLibrary>,
    dependencyGraph: Map<KotlinLibrary, Collection<KotlinLibrary>>,
    persistentCacheProviders: Map<KotlinLibrary, PersistentCacheProvider>,
    persistentCacheConsumers: Map<KotlinLibrary, PersistentCacheConsumer>
) {
    // 1. Invalidate
    val dirtyFiles: MutableMap<KotlinLibrary, Set<String>> = mutableMapOf()

    for (m in modulesRpo) {
        val libraryFiles = m.files()
        val dirtySet = persistentCacheProviders[m]?.let {
            val sigHashes = mutableMapOf<IdSignature, Hash>()
            dependencyGraph.transitiveClosure(m).forEach { d ->
                sigHashes.putAll(persistentCacheProviders[d]!!.inlineHashes())
            }
            val thisHashes = mutableMapOf<String, Map<IdSignature, Hash>>()
            libraryFiles.associateWithTo(thisHashes) { f -> it.inlineHashes(f) }
            invalidateCacheForModule(libraryFiles, sigHashes, thisHashes, it, persistentCacheConsumers[m]!!)
        } ?: libraryFiles

        dirtyFiles[m] = dirtySet
    }

    // 2. Build

    val loadedModules = loadModules(modulesRpo, dependencyGraph)

    val jsIrLinker = createLinker(loadedModules)

    val irModules = ArrayList<Pair<IrModuleFragment, KotlinLibrary>>(loadedModules.size)

    for ((descriptor, library) in loadedModules) {
        val filesToRebuild = dirtyFiles[library]!!
        irModules.add(jsIrLinker.deserializeDirtyFiles(descriptor, library, filesToRebuild) to library)
    }

    jsIrLinker.postProcess()

    for ((irModule, library) in irModules) {
        val dirtySet = dirtyFiles[library]!!

        val cleanInlineHashes = mutableMapOf<IdSignature, Hash>()
        dependencyGraph[library]!!.forEach { cleanInlineHashes.putAll(persistentCacheProviders[it]!!.inlineHashes()) }
        val thisCacheProvider = persistentCacheProviders[library]!!
        for (file in library.files()) {
            if (file !in dirtySet) {
                cleanInlineHashes.putAll(thisCacheProvider.inlineHashes(file))
            }
        }

        val cacheConsumer = persistentCacheConsumers[library] ?: error("No cache consumer found for $library")
        buildCacheForModule(irModule, dirtySet, cleanInlineHashes, cacheConsumer)
    }
}


fun compile(
    project: Project,
    mainModule: MainModule,
    analyzer: AbstractAnalyzerWithCompilerReport,
    configuration: CompilerConfiguration,
    phaseConfig: PhaseConfig,
    allDependencies: KotlinLibraryResolveResult,
    friendDependencies: List<KotlinLibrary>,
    mainArguments: List<String>?,
    exportedDeclarations: Set<FqName> = emptySet(),
    generateFullJs: Boolean = true,
    generateDceJs: Boolean = false,
    dceDriven: Boolean = false,
    es6mode: Boolean = false,
    multiModule: Boolean = false,
    relativeRequirePath: Boolean = false,
    propertyLazyInitialization: Boolean,
): CompilerResult {
    val irFactory = if (dceDriven) PersistentIrFactory() else IrFactoryImpl

    val (moduleFragment: IrModuleFragment, dependencyModules, irBuiltIns, symbolTable, deserializer) =
        loadIr(project, mainModule, analyzer, configuration, allDependencies, friendDependencies, irFactory)

    val moduleDescriptor = moduleFragment.descriptor

    val allModules = when (mainModule) {
        is MainModule.SourceFiles -> dependencyModules + listOf(moduleFragment)
        is MainModule.Klib -> dependencyModules
    }

    val context = JsIrBackendContext(
        moduleDescriptor,
        irBuiltIns,
        symbolTable,
        allModules.first(),
        exportedDeclarations,
        configuration,
        es6mode = es6mode,
        propertyLazyInitialization = propertyLazyInitialization,
        irFactory = irFactory
    )

    // Load declarations referenced during `context` initialization
    val irProviders = listOf(deserializer)
    ExternalDependenciesGenerator(symbolTable, irProviders).generateUnboundSymbolsAsDependencies()

    deserializer.postProcess()
    symbolTable.noUnboundLeft("Unbound symbols at the end of linker")

//    val inlineGraphTracker2 = InlineGraphTracker2()
//    moduleFragment.acceptVoid(inlineGraphTracker2)
//    val inlineGraphTracker = InlineGraphTracker()
//    moduleFragment.accept(inlineGraphTracker, null)
//
//    val sets = allModules.flatMap { m ->
//        m.files.map { f ->
//            inlineGraphTracker2.getInvalidationSetForDirtyFile(f.fileEntry.name)
//        }
//    }.filter { !it.isEmpty() }
//
//    val dependentSets = moduleFragment.files.map { it.fileEntry.name to inlineGraphTracker[it.fileEntry.name] }.toMap()
//
//    val invalidated = mutableMapOf<IrFile, InlineGraphTracker2>()

//    allModules.forEach { m ->
//        m.files.forEach { f ->
//            val newGraph = inlineGraphTracker2.copy()
//            if (newGraph.invalidateForFile(f.fileEntry.name)) {
//                invalidated[f] = newGraph
//            }
//        }
//    }
//
//    val sssets = sets.map { it.toString() }

    allModules.forEach { module ->
        moveBodilessDeclarationsToSeparatePlace(context, module)
    }

    // TODO should be done incrementally
    generateTests(context, allModules.last())

    if (dceDriven) {
        val controller = MutableController(context, pirLowerings)

        check(irFactory is PersistentIrFactory)
        irFactory.stageController = controller

        controller.currentStage = controller.lowerings.size + 1

        eliminateDeadDeclarations(allModules, context)

        irFactory.stageController = StageController(controller.currentStage)

        val transformer = IrModuleToJsTransformer(
            context,
            mainArguments,
            fullJs = true,
            dceJs = false,
            multiModule = multiModule,
            relativeRequirePath = relativeRequirePath
        )
        return transformer.generateModule(allModules)
    } else {
        jsPhases.invokeToplevel(phaseConfig, context, allModules)
        val transformer = IrModuleToJsTransformer(
            context,
            mainArguments,
            fullJs = generateFullJs,
            dceJs = generateDceJs,
            multiModule = multiModule,
            relativeRequirePath = relativeRequirePath
        )
        return transformer.generateModule(allModules)
    }
}

fun generateJsCode(
    context: JsIrBackendContext,
    moduleFragment: IrModuleFragment,
    nameTables: NameTables
): String {
    moveBodilessDeclarationsToSeparatePlace(context, moduleFragment)
    jsPhases.invokeToplevel(PhaseConfig(jsPhases), context, listOf(moduleFragment))

    val transformer = IrModuleToJsTransformer(context, null, true, nameTables)
    return transformer.generateModule(listOf(moduleFragment)).jsCode!!.mainModule
}
