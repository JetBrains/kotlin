/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.pipeline.web.wasm

import org.jetbrains.kotlin.backend.wasm.writeCompilationResult
import org.jetbrains.kotlin.cli.pipeline.PipelinePhase
import org.jetbrains.kotlin.cli.pipeline.web.WasmBackendPipelineArtifact
import org.jetbrains.kotlin.js.config.outputDir

object WasmWriteOutputsPipelinePhase : PipelinePhase<WasmBackendPipelineArtifact, WasmBackendPipelineArtifact>(
    name = "WasmWriteOutputsPipelinePhase"
) {
    override fun executePhase(input: WasmBackendPipelineArtifact): WasmBackendPipelineArtifact {
        (val result, val configuration) = input
        val outputDir = configuration.outputDir!!
        for (compileResult in result) {
            writeCompilationResult(
                result = compileResult,
                dir = outputDir,
                fileNameBase = compileResult.baseFileName,
                configuration = configuration,
            )
        }
        return input
    }
}
