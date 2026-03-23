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

abstract class JsAndWasmCliPipeline<T : CommonJsAndWasmCompilerArguments>(
    override val defaultPerformanceManager: PerformanceManager,
) : AbstractCliPipeline<T>() {

    abstract fun createCodeGenerationPhase(): CompilerPhase<PipelineContext, ArgumentsPipelineArtifact<T>, *>

    override fun createCompoundPhase(arguments: T): CompilerPhase<PipelineContext, ArgumentsPipelineArtifact<T>, *> {
        return when {
            arguments.includes != null -> createCodeGenerationPhase()
            else -> createKlibSerializationPhase()
        }
    }

    private fun createKlibSerializationPhase(): CompilerPhase<PipelineContext, ArgumentsPipelineArtifact<T>, JsSerializedKlibPipelineArtifact> {
        return jsAndWasmConfigurationPhase then
                JsAndWasmFrontendPipelinePhase then
                FrontendFilesForPluginsGenerationPipelinePhase() then
                JsAndWasmFir2IrPipelinePhase then
                JsAndWasmKlibInliningPipelinePhase then
                JsAndWasmKlibSerializationPipelinePhase
    }

    abstract val jsAndWasmConfigurationPhase: CompilerPhase<PipelineContext, ArgumentsPipelineArtifact<T>, ConfigurationPipelineArtifact>
}

class JsCliPipeline(defaultPerformanceManager: PerformanceManager) : JsAndWasmCliPipeline<K2JSCompilerArguments>(defaultPerformanceManager) {
    override fun createCodeGenerationPhase(): CompilerPhase<PipelineContext, ArgumentsPipelineArtifact<K2JSCompilerArguments>, *> {
        return JsConfigurationPhase then
                JsBackendPipelinePhase
    }

    override val jsAndWasmConfigurationPhase = JsConfigurationPhase
}

class WasmCliPipeline(defaultPerformanceManager: PerformanceManager) :
    JsAndWasmCliPipeline<KotlinWasmCompilerArguments>(defaultPerformanceManager) {
    override fun createCodeGenerationPhase(): CompilerPhase<PipelineContext, ArgumentsPipelineArtifact<KotlinWasmCompilerArguments>, out JsAndWasmBackendPipelineArtifact> {
        return WasmConfigurationPhase then
                WasmBackendPipelinePhase
    }

    override val jsAndWasmConfigurationPhase = WasmConfigurationPhase
}