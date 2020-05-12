/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analyzer.AbstractAnalyzerWithCompilerReport
import org.jetbrains.kotlin.backend.common.phaser.PhaseConfig
import org.jetbrains.kotlin.backend.common.phaser.invokeToplevel
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.ir.backend.js.lower.generateTests
import org.jetbrains.kotlin.ir.backend.js.lower.moveBodilessDeclarationsToSeparatePlace
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.IrModuleToJsTransformer
import org.jetbrains.kotlin.ir.backend.js.utils.JsMainFunctionDetector
import org.jetbrains.kotlin.ir.backend.js.utils.NameTables
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.StageController
import org.jetbrains.kotlin.ir.declarations.stageController
import org.jetbrains.kotlin.ir.util.DeclarationStubGenerator
import org.jetbrains.kotlin.ir.util.ExternalDependenciesGenerator
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.resolver.KotlinLibraryResolveResult
import org.jetbrains.kotlin.name.FqName

class CompilerResult(
    val jsCode: String?,
    val dceJsCode: String?,
    val tsDefinitions: String? = null
)

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
    es6mode: Boolean = false
): CompilerResult {
    stageController = object : StageController {}

    val (moduleFragment: IrModuleFragment, dependencyModules, irBuiltIns, symbolTable, deserializer) =
        loadIr(project, mainModule, analyzer, configuration, allDependencies, friendDependencies)

    val moduleDescriptor = moduleFragment.descriptor

    val mainFunction = JsMainFunctionDetector.getMainFunctionOrNull(moduleFragment)

    val context = JsIrBackendContext(moduleDescriptor, irBuiltIns, symbolTable, moduleFragment, exportedDeclarations, configuration, es6mode = es6mode)

    // Load declarations referenced during `context` initialization
    val irProviders = listOf(deserializer)
    ExternalDependenciesGenerator(symbolTable, irProviders, configuration.languageVersionSettings).generateUnboundSymbolsAsDependencies()

    val allModules = when (mainModule) {
        is MainModule.SourceFiles -> dependencyModules + listOf(moduleFragment)
        is MainModule.Klib -> dependencyModules
    }

    val irFiles = allModules.flatMap { it.files }

    deserializer.postProcess()

    moduleFragment.files.clear()
    moduleFragment.files += irFiles

    moveBodilessDeclarationsToSeparatePlace(context, moduleFragment)

    if (dceDriven) {

        // TODO we should only generate tests for the current module
        // TODO should be done incrementally
        generateTests(context, moduleFragment)

        val controller = MutableController(context, pirLowerings)
        stageController = controller

        controller.currentStage = controller.lowerings.size + 1

        eliminateDeadDeclarations(moduleFragment, context, mainFunction)

        // TODO investigate whether this is needed anymore
        stageController = object : StageController {
            override val currentStage: Int = controller.currentStage
        }

        val transformer = IrModuleToJsTransformer(context, mainFunction, mainArguments)
        return transformer.generateModule(moduleFragment, fullJs = true, dceJs = false)
    } else {
        val phases = if (es6mode) jsEs6Phases else jsPhases
        phases.invokeToplevel(phaseConfig, context, moduleFragment)
        val transformer = IrModuleToJsTransformer(context, mainFunction, mainArguments)
        return transformer.generateModule(moduleFragment, generateFullJs, generateDceJs)
    }
}

fun generateJsCode(
    context: JsIrBackendContext,
    moduleFragment: IrModuleFragment,
    nameTables: NameTables
): String {
    moveBodilessDeclarationsToSeparatePlace(context, moduleFragment)
    jsPhases.invokeToplevel(PhaseConfig(jsPhases), context, moduleFragment)

    val transformer = IrModuleToJsTransformer(context, null, null, true, nameTables)
    return transformer.generateModule(moduleFragment).jsCode!!
}
