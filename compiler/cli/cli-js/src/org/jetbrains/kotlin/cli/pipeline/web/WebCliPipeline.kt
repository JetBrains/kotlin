/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.pipeline.web

import org.jetbrains.kotlin.backend.common.phaser.then
import org.jetbrains.kotlin.cli.common.arguments.CommonJsAndWasmCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.KotlinWasmCompilerArguments
import org.jetbrains.kotlin.cli.pipeline.*
import org.jetbrains.kotlin.cli.pipeline.web.js.*
import org.jetbrains.kotlin.cli.pipeline.web.wasm.*
import org.jetbrains.kotlin.config.phaser.CompilerPhase
import org.jetbrains.kotlin.util.PerformanceManager

abstract class WebCliPipeline<T : CommonJsAndWasmCompilerArguments>(
    override val defaultPerformanceManager: PerformanceManager,
) : AbstractCliPipeline<T>() {

    abstract fun createCodeGenerationPhase(arguments: T): CompilerPhase<PipelineContext, ArgumentsPipelineArtifact<T>, *>

    override fun createCompoundPhase(arguments: T): CompilerPhase<PipelineContext, ArgumentsPipelineArtifact<T>, *> {
        return when {
            arguments.includes != null -> createCodeGenerationPhase(arguments)
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
    override fun createCodeGenerationPhase(arguments: K2JSCompilerArguments): CompilerPhase<PipelineContext, ArgumentsPipelineArtifact<K2JSCompilerArguments>, *> {
        return JsConfigurationPhase then if (arguments.cacheDirectory == null) {
            // Non-incremental compilation pipeline
            JsIrLoadingPipelinePhase then
                    JsIrLoweringPipelinePhase then
                    JsCodegenPipelinePhase then
                    JsWriteOutputsPipelinePhase
        } else {
            // Incremental compilation pipeline
            JsIncrementalCachePreparationPipelinePhase then
                    JsIncrementalBuildingPhase
        }
    }

    override val webConfigurationPhase = JsConfigurationPhase
}

class WasmCliPipeline(defaultPerformanceManager: PerformanceManager) :
    WebCliPipeline<KotlinWasmCompilerArguments>(defaultPerformanceManager) {
    override fun createCodeGenerationPhase(arguments: KotlinWasmCompilerArguments): CompilerPhase<PipelineContext, ArgumentsPipelineArtifact<KotlinWasmCompilerArguments>, out WebBackendPipelineArtifact> {
        return WasmConfigurationPhase then if (arguments.cacheDirectory == null) {
            // Non-incremental compilation pipeline
            WasmIrLoadingPipelinePhase then
                    WasmIrLinkingPipelinePhase then
                    WasmIrLoweringPipelinePhase then
                    when {
                        arguments.wasmIncludedModuleOnly -> WasmSingleModuleBackendIrGenerationPipelinePhase
                        arguments.wasmGenerateClosedWorldMultimodule -> WasmMultiModuleBackendIrGenerationPipelinePhase
                        else -> WasmWholeWorldBackendIrGenerationPipelinePhase
                    }
        } else {
            // Incremental compilation pipeline
            when {
                arguments.wasmIncludedModuleOnly ->
                    WasmSingleModuleIncrementalCachePreparationPipelinePhase then
                            WasmSingleModuleIncrementalBuildingPhase
                arguments.wasmGenerateClosedWorldMultimodule ->
                    WasmMultiModuleIncrementalCachePreparationPipelinePhase then
                            WasmMultiModuleIncrementalBuildingPhase
                else ->
                    WasmWholeWorldIncrementalCachePreparationPipelinePhase then
                            WasmWholeWorldIncrementalBuildingPhase

            }
        } then
                WasmBinaryGenerationPipelinePhase then
                WasmWriteOutputsPipelinePhase
    }

    override val webConfigurationPhase = WasmConfigurationPhase
}
