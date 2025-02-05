/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.pipeline.web

import org.jetbrains.kotlin.backend.common.phaser.then
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.pipeline.AbstractCliPipeline
import org.jetbrains.kotlin.cli.pipeline.ArgumentsPipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.PipelineContext
import org.jetbrains.kotlin.cli.pipeline.web.js.JsBackendPipelinePhase
import org.jetbrains.kotlin.cli.pipeline.web.wasm.WasmBackendPipelinePhase
import org.jetbrains.kotlin.config.phaser.CompilerPhase
import org.jetbrains.kotlin.util.CommonCompilerPerformanceManager

class WebCliPipeline(
    override val defaultPerformanceManager: CommonCompilerPerformanceManager
) : AbstractCliPipeline<K2JSCompilerArguments>() {
    override fun createCompoundPhase(arguments: K2JSCompilerArguments): CompilerPhase<PipelineContext, ArgumentsPipelineArtifact<K2JSCompilerArguments>, *> {
        return when {
            arguments.includes != null -> when {
                arguments.wasm -> createWasmCodeGenerationPhase()
                else -> createJsCodeGenerationPhase()
            }
            else -> createKlibSerializationPhase()
        }
    }

    private fun createKlibSerializationPhase(): CompilerPhase<PipelineContext, ArgumentsPipelineArtifact<K2JSCompilerArguments>, JsSerializedKlibPipelineArtifact> {
        return WebConfigurationPhase then
                WebFrontendPipelinePhase then
                WebFir2IrPipelinePhase then
                WebKlibSerializationPipelinePhase
    }

    private fun createJsCodeGenerationPhase(): CompilerPhase<PipelineContext, ArgumentsPipelineArtifact<K2JSCompilerArguments>, JsBackendPipelineArtifact> {
        return WebConfigurationPhase then
                JsBackendPipelinePhase
    }

    private fun createWasmCodeGenerationPhase(): CompilerPhase<PipelineContext, ArgumentsPipelineArtifact<K2JSCompilerArguments>, WasmBackendPipelineArtifact> {
        return WebConfigurationPhase then
                WasmBackendPipelinePhase
    }
}
