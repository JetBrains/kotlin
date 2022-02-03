/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js

import org.jetbrains.kotlin.backend.common.phaser.PhaseConfig
import org.jetbrains.kotlin.backend.common.phaser.invokeToplevel
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.backend.js.codegen.JsGenerationGranularity
import org.jetbrains.kotlin.ir.backend.js.lower.generateJsTests
import org.jetbrains.kotlin.ir.backend.js.lower.moveBodilessDeclarationsToSeparatePlace
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.JsIrLinker
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.IrModuleToJsTransformer
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.TranslationMode
import org.jetbrains.kotlin.ir.backend.js.utils.NameTables
import org.jetbrains.kotlin.ir.declarations.IrFactory
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.util.ExternalDependenciesGenerator
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.ir.util.noUnboundLeft
import org.jetbrains.kotlin.js.backend.ast.JsProgram
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.js.config.RuntimeDiagnostic
import org.jetbrains.kotlin.name.FqName

class CompilerResult(
    val outputs: Map<TranslationMode, CompilationOutputs>,
    val tsDefinitions: String? = null
)

class CompilationOutputs(
    val jsCode: String,
    val jsProgram: JsProgram? = null,
    val sourceMap: String? = null,
    val dependencies: Iterable<Pair<String, CompilationOutputs>> = emptyList()
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
    dceRuntimeDiagnostic: RuntimeDiagnostic? = null,
    es6mode: Boolean = false,
    verifySignatures: Boolean = true,
    baseClassIntoMetadata: Boolean = false,
    safeExternalBoolean: Boolean = false,
    safeExternalBooleanDiagnostic: RuntimeDiagnostic? = null,
    filesToLower: Set<String>? = null,
    granularity: JsGenerationGranularity = JsGenerationGranularity.WHOLE_PROGRAM,
    icCompatibleIr2Js: Boolean = false,
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
        dceRuntimeDiagnostic,
        es6mode,
        baseClassIntoMetadata,
        safeExternalBoolean,
        safeExternalBooleanDiagnostic,
        granularity,
        icCompatibleIr2Js,
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
    deserializer: JsIrLinker,
    phaseConfig: PhaseConfig,
    exportedDeclarations: Set<FqName>,
    dceRuntimeDiagnostic: RuntimeDiagnostic?,
    es6mode: Boolean,
    baseClassIntoMetadata: Boolean,
    safeExternalBoolean: Boolean,
    safeExternalBooleanDiagnostic: RuntimeDiagnostic?,
    granularity: JsGenerationGranularity,
    icCompatibleIr2Js: Boolean,
): LoweredIr {
    val moduleDescriptor = moduleFragment.descriptor
    val irFactory = symbolTable.irFactory

    val allModules = when (mainModule) {
        is MainModule.SourceFiles -> dependencyModules + listOf(moduleFragment)
        is MainModule.Klib -> dependencyModules
    }

    val allowUnboundSymbols = configuration[JSConfigurationKeys.PARTIAL_LINKAGE] ?: false

    val context = JsIrBackendContext(
        moduleDescriptor,
        irBuiltIns,
        symbolTable,
        allModules.first(),
        exportedDeclarations,
        configuration,
        es6mode = es6mode,
        dceRuntimeDiagnostic = dceRuntimeDiagnostic,
        baseClassIntoMetadata = baseClassIntoMetadata,
        safeExternalBoolean = safeExternalBoolean,
        safeExternalBooleanDiagnostic = safeExternalBooleanDiagnostic,
        granularity = granularity,
        icCompatibleIr2Js = icCompatibleIr2Js,
    )

    // Load declarations referenced during `context` initialization
    val irProviders = listOf(deserializer)
    ExternalDependenciesGenerator(symbolTable, irProviders).generateUnboundSymbolsAsDependencies()

    deserializer.postProcess()
    if (!allowUnboundSymbols) {
        symbolTable.noUnboundLeft("Unbound symbols at the end of linker")
    }

    allModules.forEach { module ->
        moveBodilessDeclarationsToSeparatePlace(context, module)
    }

    // TODO should be done incrementally
    generateJsTests(context, allModules.last())

    (irFactory.stageController as? WholeWorldStageController)?.let {
        lowerPreservingTags(allModules, context, phaseConfig, it)
    } ?: jsPhases.invokeToplevel(phaseConfig, context, allModules)

    return LoweredIr(context, moduleFragment, allModules, moduleToName)
}

fun generateJsCode(
    context: JsIrBackendContext,
    moduleFragment: IrModuleFragment,
    nameTables: NameTables
): String {
    moveBodilessDeclarationsToSeparatePlace(context, moduleFragment)
    jsPhases.invokeToplevel(PhaseConfig(jsPhases), context, listOf(moduleFragment))

    val transformer = IrModuleToJsTransformer(context, null, true, nameTables)
    return transformer.generateModule(listOf(moduleFragment)).outputs[TranslationMode.FULL]!!.jsCode
}