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
import org.jetbrains.kotlin.ir.backend.js.utils.NameTables
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.StageController
import org.jetbrains.kotlin.ir.declarations.persistent.PersistentIrFactory
import org.jetbrains.kotlin.ir.declarations.stageController
import org.jetbrains.kotlin.ir.util.ExternalDependenciesGenerator
import org.jetbrains.kotlin.ir.util.noUnboundLeft
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.resolver.KotlinLibraryResolveResult
import org.jetbrains.kotlin.name.FqName

class CompilerResult(
    val jsCode: JsCode?,
    val dceJsCode: JsCode?,
    val tsDefinitions: String? = null
)

class JsCode(val mainModule: String, val dependencies: Iterable<Pair<String, String>> = emptyList())

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
    stageController = StageController()

    val (moduleFragment: IrModuleFragment, dependencyModules, irBuiltIns, symbolTable, deserializer) =
        loadIr(project, mainModule, analyzer, configuration, allDependencies, friendDependencies, PersistentIrFactory)

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
    )

    // Load declarations referenced during `context` initialization
    val irProviders = listOf(deserializer)
    ExternalDependenciesGenerator(symbolTable, irProviders, configuration.languageVersionSettings).generateUnboundSymbolsAsDependencies()

    deserializer.postProcess()
    symbolTable.noUnboundLeft("Unbound symbols at the end of linker")

    allModules.forEach { module ->
        moveBodilessDeclarationsToSeparatePlace(context, module)
    }

    // TODO should be done incrementally
    generateTests(context, allModules.last())

    if (dceDriven) {
        val controller = MutableController(context, pirLowerings)
        stageController = controller

        controller.currentStage = controller.lowerings.size + 1

        eliminateDeadDeclarations(allModules, context)

        // TODO investigate whether this is needed anymore
        stageController = StageController(controller.currentStage)

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
