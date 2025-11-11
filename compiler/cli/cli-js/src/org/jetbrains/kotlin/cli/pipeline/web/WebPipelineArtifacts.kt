/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
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
import org.jetbrains.kotlin.fir.pipeline.AllModulesFrontendOutput
import org.jetbrains.kotlin.ir.backend.js.ModulesStructure
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.CompilationOutputs
import java.io.File

data class WebFrontendPipelineArtifact(
    override val frontendOutput: AllModulesFrontendOutput,
    override val configuration: CompilerConfiguration,
    override val diagnosticCollector: BaseDiagnosticsCollector,
    val moduleStructure: ModulesStructure,
    val hasErrors: Boolean,
) : FrontendPipelineArtifact() {
    override fun withNewDiagnosticCollectorImpl(newDiagnosticsCollector: BaseDiagnosticsCollector): WebFrontendPipelineArtifact {
        return copy(diagnosticCollector = newDiagnosticsCollector)
    }

    override fun withNewFrontendOutputImpl(newFrontendOutput: AllModulesFrontendOutput): FrontendPipelineArtifact {
        return copy(frontendOutput = newFrontendOutput)
    }
}

data class JsFir2IrPipelineArtifact(
    override val result: Fir2IrActualizedResult,
    val frontendOutput: AllModulesFrontendOutput,
    val configuration: CompilerConfiguration,
    override val diagnosticCollector: BaseDiagnosticsCollector,
    val moduleStructure: ModulesStructure,
    val hasErrors: Boolean,
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
