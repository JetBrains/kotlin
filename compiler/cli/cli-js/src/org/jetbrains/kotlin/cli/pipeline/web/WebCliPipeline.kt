/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.pipeline.web

import org.jetbrains.kotlin.backend.common.phaser.then
import org.jetbrains.kotlin.cli.common.arguments.CommonJsAndWasmCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.KotlinWasmCompilerArguments
import org.jetbrains.kotlin.cli.pipeline.*
import org.jetbrains.kotlin.cli.pipeline.web.js.JsBackendPipelinePhase
import org.jetbrains.kotlin.cli.pipeline.web.wasm.WasmBackendPipelinePhase
import org.jetbrains.kotlin.config.phaser.CompilerPhase
import org.jetbrains.kotlin.util.PerformanceManager

abstract class WebCliPipeline<T : CommonJsAndWasmCompilerArguments>(
    override val defaultPerformanceManager: PerformanceManager,
) : AbstractCliPipeline<T>() {

    abstract fun createCodeGenerationPhase(): CompilerPhase<PipelineContext, ArgumentsPipelineArtifact<T>, *>

    override fun createCompoundPhase(arguments: T): CompilerPhase<PipelineContext, ArgumentsPipelineArtifact<T>, *> {
        return when {
            arguments.includes != null -> createCodeGenerationPhase()
            else -> createKlibSerializationPhase()
        }
    }

    private fun createKlibSerializationPhase(): CompilerPhase<PipelineContext, ArgumentsPipelineArtifact<T>, WebSerializedKlibPipelineArtifact> {
        return webConfigurationPhase then
                WebFrontendPipelinePhase then
                FrontendFilesForPluginsGenerationPipelinePhase() then
                WebFir2IrPipelinePhase then
                WebKlibInliningPipelinePhase then
                WebKlibSerializationPipelinePhase
    }

    abstract val webConfigurationPhase: CompilerPhase<PipelineContext, ArgumentsPipelineArtifact<T>, ConfigurationPipelineArtifact>
}

class JsCliPipeline(defaultPerformanceManager: PerformanceManager) : WebCliPipeline<K2JSCompilerArguments>(defaultPerformanceManager) {
    override fun createCodeGenerationPhase(): CompilerPhase<PipelineContext, ArgumentsPipelineArtifact<K2JSCompilerArguments>, *> {
        return JsConfigurationPhase then
                JsBackendPipelinePhase
    }

    override val webConfigurationPhase = JsConfigurationPhase
}

class WasmCliPipeline(defaultPerformanceManager: PerformanceManager) :
    WebCliPipeline<KotlinWasmCompilerArguments>(defaultPerformanceManager) {
    override fun createCodeGenerationPhase(): CompilerPhase<PipelineContext, ArgumentsPipelineArtifact<KotlinWasmCompilerArguments>, out WebBackendPipelineArtifact> {
        return WasmConfigurationPhase then
                WasmBackendPipelinePhase
    }

    override val webConfigurationPhase = WasmConfigurationPhase
}
