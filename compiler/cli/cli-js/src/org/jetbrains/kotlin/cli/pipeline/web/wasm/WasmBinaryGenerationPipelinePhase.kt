/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.pipeline.web.wasm

import org.jetbrains.kotlin.backend.wasm.compileWasmIrToBinary
import org.jetbrains.kotlin.backend.wasm.linkWasmIr
import org.jetbrains.kotlin.cli.pipeline.PerformanceNotifications
import org.jetbrains.kotlin.cli.pipeline.PipelinePhase
import org.jetbrains.kotlin.cli.pipeline.web.WasmBackendPipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.web.WasmIntermediatePipelineArtifact
import org.jetbrains.kotlin.ir.backend.js.ic.tryAcquireAndRelease
import org.jetbrains.kotlin.js.config.outputDir

object WasmBinaryGenerationPipelinePhase : PipelinePhase<WasmIntermediatePipelineArtifact, WasmBackendPipelineArtifact>(
    name = "WasmBinaryGenerationPipelinePhase",
    preActions = setOf(PerformanceNotifications.BackendStarted),
    postActions = setOf(PerformanceNotifications.BackendFinished),
) {
    override fun executePhase(input: WasmIntermediatePipelineArtifact): WasmBackendPipelineArtifact {
        (val backendIr, val cacheGuard, val configuration) = input
        val outputDir = configuration.outputDir!!
        return cacheGuard.tryAcquireAndRelease {
            val results = backendIr.map { result ->
                val linkedModule = linkWasmIr(result)
                compileWasmIrToBinary(result, linkedModule)
            }
            WasmBackendPipelineArtifact(results, outputDir, configuration)
        }
    }
}
