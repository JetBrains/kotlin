/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.pipeline.web.wasm

import org.jetbrains.kotlin.backend.wasm.compileToLoweredIr
import org.jetbrains.kotlin.cli.pipeline.PerformanceNotifications
import org.jetbrains.kotlin.cli.pipeline.PipelinePhase
import org.jetbrains.kotlin.cli.pipeline.web.WasmLinkedIrPipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.web.WasmLoweredIrPipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.web.wasm.WasmCompilationMode.Companion.wasmCompilationMode
import org.jetbrains.kotlin.wasm.config.wasmDisableCrossFileOptimisations

object WasmIrLoweringPipelinePhase : PipelinePhase<WasmLinkedIrPipelineArtifact, WasmLoweredIrPipelineArtifact>(
    name = "WasmIrLoweringPipelinePhase",
    preActions = setOf(PerformanceNotifications.IrLoweringStarted),
    postActions = setOf(PerformanceNotifications.IrLoweringFinished),
) {
    override fun executePhase(input: WasmLinkedIrPipelineArtifact): WasmLoweredIrPipelineArtifact {
        (val allModules, val context = backendContext, val isWasmStdlib, val irLinker, val configuration) = input
        configuration.wasmDisableCrossFileOptimisations = configuration.wasmCompilationMode() != WasmCompilationMode.REGULAR
        val loweredIr = compileToLoweredIr(
            configuration = configuration,
            irLinker = irLinker,
            allModules = allModules,
            context = context,
        )
        return WasmLoweredIrPipelineArtifact(loweredIr, isWasmStdlib, configuration)
    }
}
