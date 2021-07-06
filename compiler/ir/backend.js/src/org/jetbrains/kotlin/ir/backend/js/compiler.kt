/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analyzer.AbstractAnalyzerWithCompilerReport
import org.jetbrains.kotlin.backend.common.lower
import org.jetbrains.kotlin.backend.common.phaser.PhaseConfig
import org.jetbrains.kotlin.backend.common.phaser.invokeToplevel
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.ir.backend.js.ic.SerializedIcData
import org.jetbrains.kotlin.ir.backend.js.ic.icCompile
import org.jetbrains.kotlin.ir.backend.js.lower.generateTests
import org.jetbrains.kotlin.ir.backend.js.lower.moveBodilessDeclarationsToSeparatePlace
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.IrModuleToJsTransformer
import org.jetbrains.kotlin.ir.backend.js.utils.NameTables
import org.jetbrains.kotlin.ir.declarations.IrFactory
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.StageController
import org.jetbrains.kotlin.ir.declarations.persistent.PersistentIrFactory
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.js.config.DceRuntimeDiagnostic
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.resolver.KotlinLibraryResolveResult
import org.jetbrains.kotlin.name.FqName

class CompilerResult(
    val outputs: CompilationOutputs?,
    val outputsAfterDce: CompilationOutputs?,
    val tsDefinitions: String? = null
)

class CompilationOutputs(
    val jsCode: String,
    val sourceMap: String? = null,
    val dependencies: Iterable<Pair<String, CompilationOutputs>> = emptyList()
)

fun compile(
    project: Project,
    mainModule: MainModule,
    analyzer: AbstractAnalyzerWithCompilerReport,
    configuration: CompilerConfiguration,
    phaseConfig: PhaseConfig,
    irFactory: IrFactory,
    allDependencies: KotlinLibraryResolveResult,
    friendDependencies: List<KotlinLibrary>,
    mainArguments: List<String>?,
    exportedDeclarations: Set<FqName> = emptySet(),
    generateFullJs: Boolean = true,
    generateDceJs: Boolean = false,
    dceDriven: Boolean = false,
    dceRuntimeDiagnostic: DceRuntimeDiagnostic? = null,
    es6mode: Boolean = false,
    multiModule: Boolean = false,
    relativeRequirePath: Boolean = false,
    propertyLazyInitialization: Boolean,
    legacyPropertyAccess: Boolean = false,
    baseClassIntoMetadata: Boolean = false,
    lowerPerModule: Boolean = false,
    useStdlibCache: Boolean = false,
    icCache: Map<String, SerializedIcData> = emptyMap(),
): CompilerResult {

    if (lowerPerModule) {
        return icCompile(
            project,
            mainModule,
            analyzer,
            configuration,
            allDependencies,
            friendDependencies,
            mainArguments,
            exportedDeclarations,
            generateFullJs,
            generateDceJs,
            dceRuntimeDiagnostic,
            es6mode,
            multiModule,
            relativeRequirePath,
            propertyLazyInitialization,
            useStdlibCache,
            icCache,
        )
    }

    val (moduleFragment: IrModuleFragment, dependencyModules, irBuiltIns, symbolTable, deserializer, moduleToName) =
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
        dceRuntimeDiagnostic = dceRuntimeDiagnostic,
        propertyLazyInitialization = propertyLazyInitialization,
        legacyPropertyAccess = legacyPropertyAccess,
        baseClassIntoMetadata = baseClassIntoMetadata
    )

    // Load declarations referenced during `context` initialization
    val irProviders = listOf(deserializer)
    ExternalDependenciesGenerator(symbolTable, irProviders).generateUnboundSymbolsAsDependencies()

    deserializer.postProcess()
    symbolTable.noUnboundLeft("Unbound symbols at the end of linker")

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
            relativeRequirePath = relativeRequirePath,
            moduleToName = moduleToName,
        )
        return transformer.generateModule(allModules)
    } else {
        // TODO is this reachable when lowerPerModule == true?
        if (lowerPerModule) {
            val controller = WholeWorldStageController()
            check(irFactory is PersistentIrFactory)
            irFactory.stageController = controller
            allModules.forEach {
                lowerPreservingIcData(it, context, controller)
            }
            irFactory.stageController = object : StageController(irFactory.stageController.currentStage) {}
        } else {
            jsPhases.invokeToplevel(phaseConfig, context, allModules)
        }

        val transformer = IrModuleToJsTransformer(
            context,
            mainArguments,
            fullJs = generateFullJs,
            dceJs = generateDceJs,
            multiModule = multiModule,
            relativeRequirePath = relativeRequirePath,
            moduleToName = moduleToName,
        )
        return transformer.generateModule(allModules)
    }
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
            // else -> TODO what about other lowerings?
        }
    }

    controller.currentStage = pirLowerings.size + 1
}

fun generateJsCode(
    context: JsIrBackendContext,
    moduleFragment: IrModuleFragment,
    nameTables: NameTables
): String {
    moveBodilessDeclarationsToSeparatePlace(context, moduleFragment)
    jsPhases.invokeToplevel(PhaseConfig(jsPhases), context, listOf(moduleFragment))

    val transformer = IrModuleToJsTransformer(context, null, true, nameTables)
    return transformer.generateModule(listOf(moduleFragment)).outputs!!.jsCode
}