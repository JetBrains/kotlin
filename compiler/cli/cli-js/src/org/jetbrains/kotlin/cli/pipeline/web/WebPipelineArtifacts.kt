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
    override val diagnosticsCollector: BaseDiagnosticsCollector,
    val moduleStructure: ModulesStructure,
    val hasErrors: Boolean,
) : FrontendPipelineArtifact() {
    @CliPipelineInternals(OPT_IN_MESSAGE)
    override fun withCompilerConfiguration(newConfiguration: CompilerConfiguration): WebFrontendPipelineArtifact {
        return copy(configuration = newConfiguration)
    }

    override fun withNewFrontendOutputImpl(newFrontendOutput: AllModulesFrontendOutput): FrontendPipelineArtifact {
        return copy(frontendOutput = newFrontendOutput)
    }
}

data class JsFir2IrPipelineArtifact(
    override val result: Fir2IrActualizedResult,
    val frontendOutput: AllModulesFrontendOutput,
    override val configuration: CompilerConfiguration,
    override val diagnosticsCollector: BaseDiagnosticsCollector,
    val moduleStructure: ModulesStructure,
    val hasErrors: Boolean,
) : Fir2IrPipelineArtifact() {
    @CliPipelineInternals(OPT_IN_MESSAGE)
    override fun withCompilerConfiguration(newConfiguration: CompilerConfiguration): JsFir2IrPipelineArtifact {
        return copy(configuration = newConfiguration)
    }
}

data class JsSerializedKlibPipelineArtifact(
    val outputKlibPath: String,
    val diagnosticsCollector: BaseDiagnosticsCollector,
    override val configuration: CompilerConfiguration,
) : PipelineArtifact() {
    @CliPipelineInternals(OPT_IN_MESSAGE)
    override fun withCompilerConfiguration(newConfiguration: CompilerConfiguration): JsSerializedKlibPipelineArtifact {
        return copy(configuration = newConfiguration)
    }
}

data class JsLoadedKlibPipelineArtifact(
    val project: Project,
    override val configuration: CompilerConfiguration,
) : PipelineArtifact() {
    @CliPipelineInternals(OPT_IN_MESSAGE)
    override fun withCompilerConfiguration(newConfiguration: CompilerConfiguration): JsLoadedKlibPipelineArtifact {
        return copy(configuration = newConfiguration)
    }
}

sealed class WebBackendPipelineArtifact : PipelineArtifact()

data class JsBackendPipelineArtifact(
    val outputs: CompilationOutputs,
    val outputDir: File,
    override val configuration: CompilerConfiguration,
) : WebBackendPipelineArtifact() {
    @CliPipelineInternals(OPT_IN_MESSAGE)
    override fun withCompilerConfiguration(newConfiguration: CompilerConfiguration): JsBackendPipelineArtifact {
        return copy(configuration = newConfiguration)
    }
}

data class WasmBackendPipelineArtifact(
    val result: List<WasmCompilerResult>,
    val outputDir: File,
    override val configuration: CompilerConfiguration
) : WebBackendPipelineArtifact() {
    @CliPipelineInternals(OPT_IN_MESSAGE)
    override fun withCompilerConfiguration(newConfiguration: CompilerConfiguration): WasmBackendPipelineArtifact {
        return copy(configuration = newConfiguration)
    }
}
