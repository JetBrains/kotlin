/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.ic

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.backend.common.lower
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.ir.backend.js.*
import org.jetbrains.kotlin.ir.backend.js.lower.generateJsTests
import org.jetbrains.kotlin.ir.backend.js.lower.moveBodilessDeclarationsToSeparatePlace
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.JsIrLinker
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.path
import org.jetbrains.kotlin.ir.declarations.persistent.PersistentIrFactory
import org.jetbrains.kotlin.ir.util.ExternalDependenciesGenerator
import org.jetbrains.kotlin.ir.util.KotlinLikeDumpOptions
import org.jetbrains.kotlin.ir.util.dumpKotlinLike
import org.jetbrains.kotlin.ir.util.noUnboundLeft
import org.jetbrains.kotlin.js.config.RuntimeDiagnostic
import org.jetbrains.kotlin.library.resolver.KotlinResolvedLibrary
import org.jetbrains.kotlin.name.FqName
import java.io.PrintWriter

fun prepareSingleLibraryIcCache(
    project: Project,
    configuration: CompilerConfiguration,
    libPath: String,
    dependencies: Collection<String>,
    friendDependencies: Collection<String> = emptyList(),
    exportedDeclarations: Set<FqName> = emptySet(),
    icCache: Map<String, ICCache> = emptyMap(),
): ICCache {
    val irFactory = PersistentIrFactory()
    val controller = WholeWorldStageController()
    irFactory.stageController = controller

    val depsDescriptor = ModulesStructure(
        project,
        MainModule.Klib(libPath),
        configuration,
        dependencies,
        friendDependencies,
        true,
        true,
        icCache
    )
    val (context, deserializer, allModules) = prepareIr(
        depsDescriptor,
        exportedDeclarations,
        null,
        false,
        false,
        irFactory,
    )

    val moduleFragment = allModules.last()

    moveBodilessDeclarationsToSeparatePlace(context, moduleFragment)

//    generateTests(context, moduleFragment)

    lowerPreservingIcData(moduleFragment, context, controller)

    return ICCache(
        PersistentCacheProvider.EMPTY,
        PersistentCacheConsumer.EMPTY,
        IcSerializer(
            context.irBuiltIns,
            context.mapping,
            irFactory,
            deserializer,
            moduleFragment
        ).serializeDeclarations(irFactory.allDeclarations)
    )
}

private fun KotlinResolvedLibrary.allDependencies(): List<KotlinResolvedLibrary> {
    val visited = mutableSetOf<KotlinResolvedLibrary>()

    val result = mutableListOf<KotlinResolvedLibrary>()

    fun KotlinResolvedLibrary.dfs() {
        visited += this

        resolvedDependencies.forEach {
            if (it !in visited) {
                it.dfs()
                result += it
            }
        }
    }

    dfs()

    return result
}

private fun dumpIr(module: IrModuleFragment, fileName: String) {
    val dumpOptions = KotlinLikeDumpOptions(printElseAsTrue = true)

    var actual = ""

    for (file in module.files) {
        actual += file.path + "\n"
        actual += run {
            var r = ""

            file.declarations.map { it.dumpKotlinLike(dumpOptions) }.sorted().forEach { r += it }

            r
        }
        actual += "\n"
    }
    PrintWriter("/home/ab/vcs/kotlin/$fileName.txt").use {
        it.print(actual)
    }
}

fun icCompile(
    depsDescriptor: ModulesStructure,
    exportedDeclarations: Set<FqName> = emptySet(),
    dceRuntimeDiagnostic: RuntimeDiagnostic? = null,
    es6mode: Boolean = false,
    propertyLazyInitialization: Boolean,
    baseClassIntoMetadata: Boolean = false,
    safeExternalBoolean: Boolean = false,
    safeExternalBooleanDiagnostic: RuntimeDiagnostic? = null,
): LoweredIr {

    val irFactory = PersistentIrFactory()
    val controller = WholeWorldStageController()
    irFactory.stageController = controller

    val (context, _, allModules, _, loweredIrLoaded) = prepareIr(
        depsDescriptor,
        exportedDeclarations,
        dceRuntimeDiagnostic,
        es6mode,
        propertyLazyInitialization,
        irFactory,
        baseClassIntoMetadata,
        safeExternalBoolean,
        safeExternalBooleanDiagnostic
    )

    val modulesToLower = allModules.filter { it !in loweredIrLoaded }

    if (!modulesToLower.isEmpty()) {
        // This won't work incrementally
        modulesToLower.forEach { module ->
            moveBodilessDeclarationsToSeparatePlace(context, module)
        }

        generateJsTests(context, modulesToLower.last())

        modulesToLower.forEach {
            lowerPreservingIcData(it, context, controller)
        }
    }

//    dumpIr(allModules.first(), "simple-dump${if (useStdlibCache) "-actual" else ""}")

    return LoweredIr(context, allModules.last(), allModules)
}

fun lowerPreservingIcData(module: IrModuleFragment, context: JsIrBackendContext, controller: WholeWorldStageController) {
    // Lower all the things
    controller.currentStage = 0

    pirLowerings.forEachIndexed { i, lowering ->
        controller.currentStage = i + 1
        when (lowering) {
            is DeclarationLowering ->
                lowering.declarationTransformer(context).lower(module)
            is BodyLowering ->
                lowering.bodyLowering(context).lower(module)
            else -> TODO("what about other lowerings?")
        }
    }

    controller.currentStage = pirLowerings.size + 1
}

private fun prepareIr(
    depsDescriptor: ModulesStructure,
    exportedDeclarations: Set<FqName> = emptySet(),
    dceRuntimeDiagnostic: RuntimeDiagnostic? = null,
    es6mode: Boolean = false,
    propertyLazyInitialization: Boolean,
    irFactory: PersistentIrFactory,
    baseClassIntoMetadata: Boolean = false,
    safeExternalBoolean: Boolean = false,
    safeExternalBooleanDiagnostic: RuntimeDiagnostic? = null,
): PreparedIr {
    val (moduleFragment: IrModuleFragment, dependencyModules, irBuiltIns, symbolTable, deserializer, moduleToName, loweredIrLoaded) =
        loadIr(depsDescriptor, irFactory, false)

    val moduleDescriptor = moduleFragment.descriptor

    val allModules = when (depsDescriptor.mainModule) {
        is MainModule.SourceFiles -> dependencyModules + listOf(moduleFragment)
        is MainModule.Klib -> dependencyModules
    }

    val context = JsIrBackendContext(
        moduleDescriptor,
        irBuiltIns,
        symbolTable,
        allModules.first(),
        exportedDeclarations,
        depsDescriptor.compilerConfiguration,
        es6mode = es6mode,
        dceRuntimeDiagnostic = dceRuntimeDiagnostic,
        propertyLazyInitialization = propertyLazyInitialization,
        baseClassIntoMetadata = baseClassIntoMetadata,
        safeExternalBoolean = safeExternalBoolean,
        safeExternalBooleanDiagnostic = safeExternalBooleanDiagnostic,
        mapping = deserializer.mapping,
    )

    // Load declarations referenced during `context` initialization
    val irProviders = listOf(deserializer)
    ExternalDependenciesGenerator(symbolTable, irProviders).generateUnboundSymbolsAsDependencies()

    deserializer.postProcess()
    symbolTable.noUnboundLeft("Unbound symbols at the end of linker")

    deserializer.loadIcIr { moveBodilessDeclarationsToSeparatePlace(context, it) }

    return PreparedIr(context, deserializer, allModules, moduleToName, loweredIrLoaded)
}

data class PreparedIr(
    val context: JsIrBackendContext,
    val linker: JsIrLinker,
    val allModules: List<IrModuleFragment>,
    val moduleToName: Map<IrModuleFragment, String>,
    val loweredIrLoaded: Set<IrModuleFragment>,
)