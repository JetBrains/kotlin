/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js

import org.jetbrains.kotlin.backend.common.phaser.PhaseConfig
import org.jetbrains.kotlin.backend.common.phaser.invokeToplevel
import org.jetbrains.kotlin.backend.common.serialization.linkerissues.checkNoUnboundSymbols
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.backend.js.codegen.JsGenerationGranularity
import org.jetbrains.kotlin.ir.backend.js.lower.collectNativeImplementations
import org.jetbrains.kotlin.ir.backend.js.lower.generateJsTests
import org.jetbrains.kotlin.ir.backend.js.lower.moveBodilessDeclarationsToSeparatePlace
import org.jetbrains.kotlin.ir.backend.web.lower.serialization.ir.JsIrLinker
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.CompilationOutputs
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.TranslationMode
import org.jetbrains.kotlin.ir.backend.web.MainModule
import org.jetbrains.kotlin.ir.backend.web.ModulesStructure
import org.jetbrains.kotlin.ir.backend.web.loadIr
import org.jetbrains.kotlin.ir.declarations.IrFactory
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.util.ExternalDependenciesGenerator
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.js.config.RuntimeDiagnostic
import org.jetbrains.kotlin.name.FqName

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
    depsDescriptors: ModulesStructure,
    phaseConfig: PhaseConfig,
    irFactory: IrFactory,
    exportedDeclarations: Set<FqName> = emptySet(),
    keep: Set<String> = emptySet(),
    dceRuntimeDiagnostic: RuntimeDiagnostic? = null,
    es6mode: Boolean = false,
    verifySignatures: Boolean = true,
    safeExternalBoolean: Boolean = false,
    safeExternalBooleanDiagnostic: RuntimeDiagnostic? = null,
    filesToLower: Set<String>? = null,
    granularity: JsGenerationGranularity = JsGenerationGranularity.WHOLE_PROGRAM,
): LoweredIr {

    val (moduleFragment: IrModuleFragment, dependencyModules, irBuiltIns, symbolTable, deserializer, moduleToName) =
        loadIr(depsDescriptors, irFactory, verifySignatures, filesToLower, loadFunctionInterfacesIntoStdlib = true)

    return compileIr(
        moduleFragment,
        depsDescriptors.mainModule,
        depsDescriptors.compilerConfiguration,
        dependencyModules,
        moduleToName,
        irBuiltIns,
        symbolTable,
        deserializer,
        phaseConfig,
        exportedDeclarations,
        keep,
        dceRuntimeDiagnostic,
        es6mode,
        safeExternalBoolean,
        safeExternalBooleanDiagnostic,
        granularity,
    )
}

fun compileIr(
    moduleFragment: IrModuleFragment,
    mainModule: MainModule,
    configuration: CompilerConfiguration,
    dependencyModules: List<IrModuleFragment>,
    moduleToName: Map<IrModuleFragment, String>,
    irBuiltIns: IrBuiltIns,
    symbolTable: SymbolTable,
    irLinker: JsIrLinker,
    phaseConfig: PhaseConfig,
    exportedDeclarations: Set<FqName>,
    keep: Set<String>,
    dceRuntimeDiagnostic: RuntimeDiagnostic?,
    es6mode: Boolean,
    safeExternalBoolean: Boolean,
    safeExternalBooleanDiagnostic: RuntimeDiagnostic?,
    granularity: JsGenerationGranularity,
): LoweredIr {
    val moduleDescriptor = moduleFragment.descriptor
    val irFactory = symbolTable.irFactory
    val shouldGeneratePolyfills = configuration.getBoolean(JSConfigurationKeys.GENERATE_POLYFILLS)

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
        es6mode = es6mode,
        dceRuntimeDiagnostic = dceRuntimeDiagnostic,
        safeExternalBoolean = safeExternalBoolean,
        safeExternalBooleanDiagnostic = safeExternalBooleanDiagnostic,
        granularity = granularity,
        incrementalCacheEnabled = false
    )

    // Load declarations referenced during `context` initialization
    val irProviders = listOf(irLinker)
    ExternalDependenciesGenerator(symbolTable, irProviders).generateUnboundSymbolsAsDependencies()

    irLinker.postProcess()
    irLinker.checkNoUnboundSymbols(symbolTable, "at the end of IR linkage process")

    allModules.forEach { module ->
        if (shouldGeneratePolyfills) {
            collectNativeImplementations(context, module)
        }

        moveBodilessDeclarationsToSeparatePlace(context, module)
    }

    // TODO should be done incrementally
    generateJsTests(context, allModules.last())

    (irFactory.stageController as? WholeWorldStageController)?.let {
        lowerPreservingTags(allModules, context, phaseConfig, it)
    } ?: jsPhases.invokeToplevel(phaseConfig, context, allModules)

    return LoweredIr(context, moduleFragment, allModules, moduleToName)
}
