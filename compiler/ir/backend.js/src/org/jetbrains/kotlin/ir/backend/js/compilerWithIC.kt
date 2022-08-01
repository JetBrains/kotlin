/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js

import org.jetbrains.kotlin.backend.common.phaser.PhaseConfig
import org.jetbrains.kotlin.backend.common.phaser.PhaserState
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.backend.js.lower.collectNativeImplementations
import org.jetbrains.kotlin.ir.backend.js.lower.generateJsTests
import org.jetbrains.kotlin.ir.backend.js.lower.moveBodilessDeclarationsToSeparatePlace
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.JsIrLinker
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.JsUnlinkedDeclarationsSupport
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.*
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.util.ExternalDependenciesGenerator
import org.jetbrains.kotlin.ir.util.noUnboundLeft
import org.jetbrains.kotlin.js.config.RuntimeDiagnostic
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi2ir.descriptors.IrBuiltInsOverDescriptors

@Suppress("UNUSED_PARAMETER")
@OptIn(ObsoleteDescriptorBasedAPI::class)
fun compileWithIC(
    mainModule: IrModuleFragment,
    configuration: CompilerConfiguration,
    deserializer: JsIrLinker,
    allModules: Collection<IrModuleFragment>,
    filesToLower: Collection<IrFile>,
    mainArguments: List<String>? = null,
    exportedDeclarations: Set<FqName> = emptySet(),
    generateFullJs: Boolean = true,
    generateDceJs: Boolean = false,
    dceDriven: Boolean = false,
    dceRuntimeDiagnostic: RuntimeDiagnostic? = null,
    es6mode: Boolean = false,
    multiModule: Boolean = false,
    relativeRequirePath: Boolean = false,
    verifySignatures: Boolean = true,
    baseClassIntoMetadata: Boolean = false,
    lowerPerModule: Boolean = false,
    safeExternalBoolean: Boolean = false,
    safeExternalBooleanDiagnostic: RuntimeDiagnostic? = null
): List<JsIrFragmentAndBinaryAst> {
    val irBuiltIns = mainModule.irBuiltins
    val symbolTable = (irBuiltIns as IrBuiltInsOverDescriptors).symbolTable

    val context = JsIrBackendContext(
        mainModule.descriptor,
        irBuiltIns,
        symbolTable,
        mainModule,
        exportedDeclarations,
        configuration,
        es6mode = es6mode,
        dceRuntimeDiagnostic = dceRuntimeDiagnostic,
        baseClassIntoMetadata = baseClassIntoMetadata,
        safeExternalBoolean = safeExternalBoolean,
        safeExternalBooleanDiagnostic = safeExternalBooleanDiagnostic,
        icCompatibleIr2Js = IcCompatibleIr2Js.IC_MODE
    )

    with(deserializer.unlinkedDeclarationsSupport) {
        if (allowUnboundSymbols && this is JsUnlinkedDeclarationsSupport) {
            getLocalClassName = context.localClassDataStorage
        }
    }

    // Load declarations referenced during `context` initialization
    val irProviders = listOf(deserializer)
    ExternalDependenciesGenerator(symbolTable, irProviders).generateUnboundSymbolsAsDependencies()

    deserializer.postProcess()
    symbolTable.noUnboundLeft("Unbound symbols at the end of linker")

    allModules.forEach {
        collectNativeImplementations(context, it)
        moveBodilessDeclarationsToSeparatePlace(context, it)
    }

    generateJsTests(context, mainModule)

    lowerPreservingTags(allModules, context, PhaseConfig(jsPhases), symbolTable.irFactory.stageController as WholeWorldStageController)

    val transformer = IrModuleToJsTransformerTmp(
        context,
        mainArguments,
        relativeRequirePath = relativeRequirePath,
    )

    return transformer.generateBinaryAst(filesToLower, allModules)
}

fun lowerPreservingTags(
    modules: Iterable<IrModuleFragment>,
    context: JsIrBackendContext,
    phaseConfig: PhaseConfig,
    controller: WholeWorldStageController
) {
    // Lower all the things
    controller.currentStage = 0

    val phaserState = PhaserState<Iterable<IrModuleFragment>>()

    loweringList.forEachIndexed { i, lowering ->
        controller.currentStage = i + 1
        lowering.modulePhase.invoke(phaseConfig, phaserState, context, modules)
    }

    controller.currentStage = pirLowerings.size + 1
}
