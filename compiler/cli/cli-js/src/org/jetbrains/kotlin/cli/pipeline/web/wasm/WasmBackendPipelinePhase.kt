/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.pipeline.web.wasm

import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.backend.wasm.compileWasmIrToBinary
import org.jetbrains.kotlin.backend.wasm.ic.WasmICContextBase
import org.jetbrains.kotlin.backend.wasm.linkWasmIr
import org.jetbrains.kotlin.backend.wasm.writeCompilationResult
import org.jetbrains.kotlin.cli.pipeline.executePhaseIsolatedWithActions
import org.jetbrains.kotlin.cli.pipeline.web.*
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.perfManager
import org.jetbrains.kotlin.ir.backend.js.ic.IrICProgramFragments
import org.jetbrains.kotlin.ir.backend.js.ic.ModuleArtifact
import org.jetbrains.kotlin.ir.backend.js.ic.SrcFileArtifact
import org.jetbrains.kotlin.js.config.outputDir
import org.jetbrains.kotlin.util.PhaseType
import org.jetbrains.kotlin.util.tryMeasurePhaseTime

abstract class WasmBackendPipelinePhase<TModuleArtifact, TFileArtifact, TFragments, TIcContext> : WebBackendPipelinePhase<
        WasmBackendPipelineArtifact,
        WasmIntermediatePipelineArtifact,
        TModuleArtifact,
        TFileArtifact,
        TFragments,
        WasmBackendContext
        >(name = "WasmBackendPipelinePhase")
        where TModuleArtifact : ModuleArtifact,
              TFileArtifact : SrcFileArtifact,
              TFragments : IrICProgramFragments,
              TIcContext : WasmICContextBase<TModuleArtifact, TFileArtifact, TFragments> {
    override val klibLoadingPhase: WebIrLoadingPipelinePhase
        get() = WasmIrLoadingPipelinePhase

    protected abstract val backendIrGenerationPhase: WasmBackendIrGenerationPipelinePhase

    override fun compileIntermediate(
        intermediateResult: WasmIntermediatePipelineArtifact,
        configuration: CompilerConfiguration,
    ): WasmBackendPipelineArtifact = configuration.perfManager.tryMeasurePhaseTime(PhaseType.Backend) {
        val outputDir = configuration.outputDir!!
        val results = intermediateResult.backendIr.map { result ->
            val linkedModule = linkWasmIr(result)
            val compileResult = compileWasmIrToBinary(result, linkedModule)
            writeCompilationResult(
                result = compileResult,
                dir = outputDir,
                fileNameBase = result.baseFileName,
                configuration = configuration,
            )
            compileResult
        }
        WasmBackendPipelineArtifact(results, outputDir, configuration)
    }

    override fun compileNonIncrementally(loadedIrArtifact: WebLoadedIrPipelineArtifact): WasmIntermediatePipelineArtifact? {
        val linkedIr = WasmIrLinkingPipelinePhase.executePhaseIsolatedWithActions(loadedIrArtifact) ?: return null
        val loweredIr = WasmIrLoweringPipelinePhase.executePhaseIsolatedWithActions(linkedIr) ?: return null
        return backendIrGenerationPhase.executePhaseIsolatedWithActions(loweredIr)
    }
}
