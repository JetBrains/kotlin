/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.pipeline.web

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.backend.wasm.WasmCompilerResult
import org.jetbrains.kotlin.cli.pipeline.Fir2IrPipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.FrontendPipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.PipelineArtifact
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.fir.pipeline.Fir2IrActualizedResult
import org.jetbrains.kotlin.fir.pipeline.FirResult
import org.jetbrains.kotlin.ir.backend.js.ModulesStructure
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.CompilationOutputs
import java.io.File

data class WebFrontendPipelineArtifact(
    val analyzedOutput: AnalyzedFirOutput,
    val configuration: CompilerConfiguration,
    val diagnosticCollector: BaseDiagnosticsCollector,
    val moduleStructure: ModulesStructure,
) : FrontendPipelineArtifact() {
    override val result: FirResult
        get() = FirResult(analyzedOutput.output)
}

data class JsFir2IrPipelineArtifact(
    override val result: Fir2IrActualizedResult,
    val analyzedFirOutput: AnalyzedFirOutput,
    val configuration: CompilerConfiguration,
    val diagnosticCollector: BaseDiagnosticsCollector,
    val moduleStructure: ModulesStructure,
) : Fir2IrPipelineArtifact()

data class JsSerializedKlibPipelineArtifact(
    val outputKlibPath: String,
    val diagnosticsCollector: BaseDiagnosticsCollector,
    val configuration: CompilerConfiguration,
) : PipelineArtifact()

data class JsLoadedKlibPipelineArtifact(
    val project: Project,
    val configuration: CompilerConfiguration,
) : PipelineArtifact()

sealed class WebBackendPipelineArtifact : PipelineArtifact()

data class JsBackendPipelineArtifact(
    val outputs: CompilationOutputs,
    val outputDir: File,
    val configuration: CompilerConfiguration,
) : WebBackendPipelineArtifact()

data class WasmBackendPipelineArtifact(
    val result: WasmCompilerResult,
    val outputDir: File,
    val configuration: CompilerConfiguration
) : WebBackendPipelineArtifact()
