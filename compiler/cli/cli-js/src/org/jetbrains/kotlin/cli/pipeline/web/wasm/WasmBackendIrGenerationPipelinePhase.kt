/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.pipeline.web.wasm

import org.jetbrains.kotlin.backend.common.serialization.kotlinLibrary
import org.jetbrains.kotlin.backend.wasm.LoweredIrWithExtraArtifacts
import org.jetbrains.kotlin.backend.wasm.dce.eliminateDeadDeclarations
import org.jetbrains.kotlin.backend.wasm.ic.IrFactoryImplForWasmIC
import org.jetbrains.kotlin.cli.pipeline.PerformanceNotifications
import org.jetbrains.kotlin.cli.pipeline.PipelinePhase
import org.jetbrains.kotlin.cli.pipeline.web.WasmIntermediatePipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.web.WasmLoweredIrPipelineArtifact
import org.jetbrains.kotlin.ir.backend.js.dce.DceDumpNameCache
import org.jetbrains.kotlin.ir.backend.js.dce.dumpDeclarationIrSizesIfNeed
import org.jetbrains.kotlin.js.config.dce
import org.jetbrains.kotlin.library.isWasmStdlib
import org.jetbrains.kotlin.wasm.config.dceDumpDeclarationIrSizesToFile

abstract class WasmBackendIrGenerationPipelinePhase(name: String) :
    PipelinePhase<WasmLoweredIrPipelineArtifact, WasmIntermediatePipelineArtifact>(
        name = name,
        preActions = setOf(PerformanceNotifications.BackendStarted),
        postActions = setOf(PerformanceNotifications.BackendFinished),
    ) {
}

private val LoweredIrWithExtraArtifacts.irFactory: IrFactoryImplForWasmIC
    get() = backendContext.irFactory as IrFactoryImplForWasmIC

object WasmMultiModuleBackendIrGenerationPipelinePhase :
    WasmBackendIrGenerationPipelinePhase("WasmMultiModuleBackendIrGenerationPipelinePhase") {
    override fun executePhase(input: WasmLoweredIrPipelineArtifact): WasmIntermediatePipelineArtifact {
        (val loweredIr, val configuration) = input
        val allModules = loweredIr.loweredIr

        val dceDumpNameCache = DceDumpNameCache()
        if (configuration.dce) {
            eliminateDeadDeclarations(loweredIr.loweredIr, loweredIr.backendContext, dceDumpNameCache)
        }
        dumpDeclarationIrSizesIfNeed(configuration.dceDumpDeclarationIrSizesToFile, allModules, dceDumpNameCache)

        val backendIr = allModules.map { currentModule ->
            compileSingleModuleToWasmIr(
                configuration = configuration,
                loweredIr = loweredIr,
                signatureRetriever = loweredIr.irFactory,
                stdlibIsMainModule = currentModule.kotlinLibrary?.isWasmStdlib == true,
                mainModuleFragment = currentModule,
                typeTracking = true,
            )
        }

        return WasmIntermediatePipelineArtifact(backendIr, null, configuration)
    }
}

object WasmSingleModuleBackendIrGenerationPipelinePhase :
    WasmBackendIrGenerationPipelinePhase("WasmSingleModuleBackendIrGenerationPipelinePhase") {
    override fun executePhase(input: WasmLoweredIrPipelineArtifact): WasmIntermediatePipelineArtifact {
        (val loweredIr, val isWasmStdlib, val configuration) = input
        val irModuleConfiguration = compileSingleModuleToWasmIr(
            configuration = configuration,
            loweredIr = loweredIr,
            signatureRetriever = loweredIr.irFactory,
            stdlibIsMainModule = isWasmStdlib,
            mainModuleFragment = loweredIr.backendContext.irModuleFragment,
            typeTracking = false,
        )
        return WasmIntermediatePipelineArtifact(listOf(irModuleConfiguration), null, configuration)
    }
}

object WasmWholeWorldBackendIrGenerationPipelinePhase :
    WasmBackendIrGenerationPipelinePhase("WasmWholeWorldBackendIrGenerationPipelinePhase") {
    override fun executePhase(input: WasmLoweredIrPipelineArtifact): WasmIntermediatePipelineArtifact {
        (val loweredIr, val configuration) = input
        val dceDumpNameCache = DceDumpNameCache()
        if (configuration.dce) {
            eliminateDeadDeclarations(loweredIr.loweredIr, loweredIr.backendContext, dceDumpNameCache)
        }
        dumpDeclarationIrSizesIfNeed(configuration.dceDumpDeclarationIrSizesToFile, loweredIr.loweredIr, dceDumpNameCache)

        val irModuleConfiguration = compileWholeProgramModeToWasmIr(
            configuration = configuration,
            idSignatureRetriever = loweredIr.irFactory,
            loweredIr = loweredIr,
        )

        return WasmIntermediatePipelineArtifact(listOf(irModuleConfiguration), null, configuration)
    }
}
