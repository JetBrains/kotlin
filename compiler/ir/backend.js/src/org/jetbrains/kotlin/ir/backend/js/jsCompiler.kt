/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js

import org.jetbrains.kotlin.backend.common.linkage.issues.checkNoUnboundSymbols
import org.jetbrains.kotlin.backend.common.serialization.KotlinIrLinker
import org.jetbrains.kotlin.config.phaser.PhaserState
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.backend.js.lower.*
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.JsIrLinker
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.CompilationOutputs
import org.jetbrains.kotlin.backend.js.JsGenerationGranularity
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.TranslationMode
import org.jetbrains.kotlin.ir.declarations.IrFactory
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.util.ExternalDependenciesGenerator
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.js.config.RuntimeDiagnostic
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.util.PhaseType
import org.jetbrains.kotlin.util.PotentiallyIncorrectPhaseTimeMeasurement
import org.jetbrains.kotlin.util.tryMeasurePhaseTime

class CompilerResult(
    val outputs: Map<TranslationMode, CompilationOutputs>,
)

class LoweredIr(
    val context: JsIrBackendContext,
    val mainModule: IrModuleFragment,
    val allModules: List<IrModuleFragment>,
    val moduleFragmentToUniqueName: Map<IrModuleFragment, String>,
)

fun compile(
    mainCallArguments: List<String>?,
    modulesStructure: ModulesStructure,
    irFactory: IrFactory,
    exportedDeclarations: Set<FqName> = emptySet(),
    keep: Set<String> = emptySet(),
    dceRuntimeDiagnostic: RuntimeDiagnostic? = null,
    safeExternalBoolean: Boolean = false,
    safeExternalBooleanDiagnostic: RuntimeDiagnostic? = null,
    filesToLower: Set<String>? = null,
    granularity: JsGenerationGranularity = JsGenerationGranularity.WHOLE_PROGRAM,
): LoweredIr {
    val (moduleFragment: IrModuleFragment, dependencyModules, irBuiltIns, symbolTable, deserializer, moduleToName) =
        loadIr(modulesStructure, irFactory, filesToLower, loadFunctionInterfacesIntoStdlib = true)

    return compileIr(
        moduleFragment,
        modulesStructure.mainModule,
        mainCallArguments,
        modulesStructure.compilerConfiguration,
        dependencyModules,
        moduleToName,
        irBuiltIns,
        symbolTable,
        deserializer,
        exportedDeclarations,
        keep,
        dceRuntimeDiagnostic,
        safeExternalBoolean,
        safeExternalBooleanDiagnostic,
        granularity,
    )
}

fun compileIr(
    moduleFragment: IrModuleFragment,
    mainModule: MainModule,
    mainCallArguments: List<String>?,
    configuration: CompilerConfiguration,
    dependencyModules: List<IrModuleFragment>,
    moduleToName: Map<IrModuleFragment, String>,
    irBuiltIns: IrBuiltIns,
    symbolTable: SymbolTable,
    irLinker: KotlinIrLinker,
    exportedDeclarations: Set<FqName>,
    keep: Set<String>,
    dceRuntimeDiagnostic: RuntimeDiagnostic?,
    safeExternalBoolean: Boolean,
    safeExternalBooleanDiagnostic: RuntimeDiagnostic?,
    granularity: JsGenerationGranularity,
): LoweredIr {
    require(irLinker is JsIrLinker) {
        "jsCompiler needs JsIrLinker, but got ${irLinker.javaClass.name}"
    }
    val moduleDescriptor = moduleFragment.descriptor
    val irFactory = symbolTable.irFactory
    val shouldGeneratePolyfills = configuration.getBoolean(JSConfigurationKeys.GENERATE_POLYFILLS)
    val performanceManager = configuration[CLIConfigurationKeys.PERF_MANAGER]

    val allModules = when (mainModule) {
        is MainModule.SourceFiles -> dependencyModules + listOf(moduleFragment)
        is MainModule.Klib -> dependencyModules
    }

    val context = JsIrBackendContext(
        moduleDescriptor,
        irBuiltIns,
        symbolTable,
        exportedDeclarations,
        keep,
        configuration,
        dceRuntimeDiagnostic = dceRuntimeDiagnostic,
        safeExternalBoolean = safeExternalBoolean,
        safeExternalBooleanDiagnostic = safeExternalBooleanDiagnostic,
        granularity = granularity,
        incrementalCacheEnabled = false,
        mainCallArguments = mainCallArguments
    )

    // Load declarations referenced during `context` initialization
    val irProviders = listOf(irLinker)
    ExternalDependenciesGenerator(symbolTable, irProviders).generateUnboundSymbolsAsDependencies()

    irLinker.postProcess(inOrAfterLinkageStep = true)
    irLinker.checkNoUnboundSymbols(symbolTable, "at the end of IR linkage process")
    irLinker.clear()

    // TODO: sort modules in the reverse-topological order (use IR linker for extract information about dependencies)

    allModules.forEach { module ->
        if (shouldGeneratePolyfills) {
            collectNativeImplementations(context, module)
        }

        moveBodilessDeclarationsToSeparatePlace(context, module)
    }

    // TODO should be done incrementally
    generateJsTests(context, allModules.last())
    @OptIn(PotentiallyIncorrectPhaseTimeMeasurement::class)
    performanceManager?.notifyCurrentPhaseFinishedIfNeeded() // It should be `notifyTranslationToIRFinished`, but this phase not always started or already finished

    performanceManager.tryMeasurePhaseTime(PhaseType.IrLowering) {
        (irFactory.stageController as? WholeWorldStageController)?.let {
            lowerPreservingTags(allModules, context, it)
        } ?: run {
            val phaserState = PhaserState()
            getJsLowerings(configuration).forEachIndexed { _, lowering ->
                allModules.forEach { module ->
                    lowering.invoke(context.phaseConfig, phaserState, context, module)
                }
            }
        }
    }

    return LoweredIr(context, moduleFragment, allModules, moduleToName)
}
