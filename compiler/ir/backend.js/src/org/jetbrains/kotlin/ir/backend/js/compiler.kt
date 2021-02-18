/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("UNUSED_VARIABLE")

package org.jetbrains.kotlin.ir.backend.js

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analyzer.AbstractAnalyzerWithCompilerReport
import org.jetbrains.kotlin.backend.common.lower.inline.InlineGraphTracker
import org.jetbrains.kotlin.backend.common.lower.inline.InlineGraphTracker2
import org.jetbrains.kotlin.backend.common.phaser.PhaseConfig
import org.jetbrains.kotlin.backend.common.phaser.invokeToplevel
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.ir.backend.js.lower.generateTests
import org.jetbrains.kotlin.ir.backend.js.lower.moveBodilessDeclarationsToSeparatePlace
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.IrModuleToJsTransformer
import org.jetbrains.kotlin.ir.backend.js.utils.NameTables
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.StageController
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.declarations.persistent.PersistentIrFactory
import org.jetbrains.kotlin.ir.util.ExternalDependenciesGenerator
import org.jetbrains.kotlin.ir.util.noUnboundLeft
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.SerializedIrFile
import org.jetbrains.kotlin.library.resolver.KotlinLibraryResolveResult
import org.jetbrains.kotlin.name.FqName
import java.security.MessageDigest

class CompilerResult(
    val jsCode: JsCode?,
    val dceJsCode: JsCode?,
    val tsDefinitions: String? = null
)

class JsCode(val mainModule: String, val dependencies: Iterable<Pair<String, String>> = emptyList())


interface PersistentIrCache {
    fun invalidateForFile(path: String)

    fun updateFingerPrint(path: String, fingerprint: Long)

    fun md5ForFile(path: String): Long

    fun serializedParts(path: String): SerializedIrFile


    companion object {
        object Empty : PersistentIrCache {
            override fun invalidateForFile(path: String) {}
            override fun updateFingerPrint(path: String, fingerprint: Long) {}
            override fun md5ForFile(path: String): Long = 42L
            override fun serializedParts(path: String): SerializedIrFile {
                val zero = ByteArray(0)
                return SerializedIrFile(zero, "", path, zero, zero, zero, zero, zero)
            }
        }
    }
}

fun PersistentIrCache.buildForFile(file: IrFile) {
    TODO("Build cache for ${file.fileEntry.name}")
}

private fun ByteArray.md5(): Long {
    val d = MessageDigest.getInstance("MD5").digest(this)!!
    return ((d[0].toLong() and 0xFFL)
            or ((d[1].toLong() and 0xFFL) shl 8)
            or ((d[2].toLong() and 0xFFL) shl 16)
            or ((d[3].toLong() and 0xFFL) shl 24)
            or ((d[4].toLong() and 0xFFL) shl 32)
            or ((d[5].toLong() and 0xFFL) shl 40)
            or ((d[6].toLong() and 0xFFL) shl 48)
            or ((d[7].toLong() and 0xFFL) shl 56))
}

fun SerializedIrFile.fingerprint(): Long {
    return types.md5() xor signatures.md5() xor strings.md5() xor declarations.md5() xor bodies.md5()
}


fun <T> Iterable<T>.isIntersectedWith(other: Set<T>): Boolean = any { it in other }

fun rebuildCache(
    moduleFragment: IrModuleFragment,
    cache: PersistentIrCache,
    inlineGraphTracker: InlineGraphTracker,
    module2changedFiles: Map<IrModuleFragment, Set<String>>
): Set<String> {
    // TODO: How to detect deleted files?

    val dirtyFiles = mutableSetOf<String>()

    val currentModulePaths = moduleFragment.files.map { it.fileEntry.name }

    for (file in currentModulePaths) {

        // 1. get cached fingerprints
        val fileOldFingerprint = cache.md5ForFile(file)

        // 2. calculate new fingerprints
        val fileNewFingerprint = cache.serializedParts(file).fingerprint()

        if (fileOldFingerprint != fileNewFingerprint) {
            cache.updateFingerPrint(file, fileNewFingerprint)

            // 3. form initial dirty set
            dirtyFiles.add(file)
        }
    }

    // TODO: rethink using fingerprint of inlined files

    // TODO: increase granularity level from file to inline function

    // 4. Take into account changes from dependencies
    for (file in currentModulePaths) {
        val inlineSet = inlineGraphTracker[file]

        if (module2changedFiles.any { it.value.isIntersectedWith(inlineSet) }) {
            dirtyFiles.add(file)
        }
    }

    var dirtySetSize: Int

    // 5. expand dirty inline functions effect across the module
    do {
        dirtySetSize = dirtyFiles.size

        for (file in currentModulePaths) {
            val inlineSet = inlineGraphTracker[file]
            if (dirtyFiles.isIntersectedWith(inlineSet)) {
                dirtyFiles.add(file)
            }
        }

    } while (dirtySetSize != dirtyFiles.size)

    // 6. invalidate caches
    for (dirty in dirtyFiles) {
        cache.invalidateForFile(dirty) // persistent cache
        inlineGraphTracker.invalidateForFile(dirty) // inline graph
    }

    // 7. rebuild persistent caches
    for (dirty in dirtyFiles) {
        val irFile = moduleFragment.files.single { it.fileEntry.name == dirty }
        cache.buildForFile(irFile)
    }

    return dirtyFiles
}

//fun rebuildCache2(moduleFragment: IrModuleFragment, cache: PersistentIrCache, inlineGraphTracker: InlineGraphTracker2): Set<String> {
//    // TODO: How to detect deleted files?
//    // TODO: How to organize inline graph?
//    val dirtyFiles = mutableListOf<String>()
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
//    val extraDirtySet = mutableSetOf<String>()
//
//    // 4. extend it with inline-function dirty set
//    for (dirty in dirtyFiles) {
//        // TODO: increase granularity level from file to inline function
//        extraDirtySet.addAll(inlineGraphTracker.getInvalidationSetForDirtyFile(dirty).files)
//        inlineGraphTracker.invalidateForFile(dirty)
//    }
//
//    val (current, others) = extraDirtySet.partition { it in currentModulePaths }
//
//    dirtyFiles.addAll(current)
//
//    // 5. invalidate persistent caches for files in the extended set
//    for (dirty in dirtyFiles) {
//        cache.invalidateForFile(dirty)
//    }
//
//    // 6. rebuild persistent cache
//    for (dirty in dirtyFiles) {
//        val irFile = moduleFragment.files.single { it.fileEntry.name == dirty }
//        cache.buildForFile(irFile)
//    }
//
//    // 7. return set of files which affected by inline functions changed but from current module
//    return others.toSet()
//}

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

    val inlineGraphTracker2 = InlineGraphTracker2()
    moduleFragment.acceptVoid(inlineGraphTracker2)
    val inlineGraphTracker = InlineGraphTracker()
    moduleFragment.accept(inlineGraphTracker, null)

    val sets = allModules.flatMap { m ->
        m.files.map { f ->
            inlineGraphTracker2.getInvalidationSetForDirtyFile(f.fileEntry.name)
        }
    }.filter { !it.isEmpty() }

    val dependentSets = moduleFragment.files.map { it.fileEntry.name to inlineGraphTracker[it.fileEntry.name] }.toMap()

    val invalidated = mutableMapOf<IrFile, InlineGraphTracker2>()

    allModules.forEach { m ->
        m.files.forEach { f ->
            val newGraph = inlineGraphTracker2.copy()
            if (newGraph.invalidateForFile(f.fileEntry.name)) {
                invalidated[f] = newGraph
            }
        }
    }

    val sssets = sets.map { it.toString() }

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
