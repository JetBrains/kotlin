/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.native.pipeline

import org.jetbrains.kotlin.backend.common.phaser.then
import org.jetbrains.kotlin.backend.common.phaser.thenIf
import org.jetbrains.kotlin.cli.common.arguments.K2NativeCompilerArguments
import org.jetbrains.kotlin.cli.pipeline.AbstractCliPipeline
import org.jetbrains.kotlin.cli.pipeline.ArgumentsPipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.PipelineContext
import org.jetbrains.kotlin.config.AnalysisFlags
import org.jetbrains.kotlin.config.HeaderMode
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.config.phaser.CompilerPhase
import org.jetbrains.kotlin.fir.pipeline.requireIrForHeaderCompilationMode
import org.jetbrains.kotlin.util.PerformanceManager

class NativeKlibCliPipeline(
    override val defaultPerformanceManager: PerformanceManager,
    override val isNativeOneStage: Boolean,
) : AbstractCliPipeline<K2NativeCompilerArguments>() {

    override fun createCompoundPhase(
        arguments: K2NativeCompilerArguments
    ): CompilerPhase<PipelineContext, ArgumentsPipelineArtifact<K2NativeCompilerArguments>, *> {
        return NativeConfigurationPhase then
                NativeFrontendPipelinePhase.thenIf(
                    condition = ::skipIrGeneration,
                    onTrue = NativeMetadataSerializationPipelinePhase,
                    onFalse = NativeFir2IrPipelinePhase then
                            NativePreSerializationPipelinePhase then
                            NativeIrSerializationPipelinePhase
                ) then
                NativeKlibWritingPipelinePhase
    }

    /**
     * Skips IR generation if either generating using -Xmetadata-klib or running header mode with
     * -Xheader-mode and -Xheader-mode-type=compilation.
     */
    private fun skipIrGeneration(artifact: NativeFrontendArtifact): Boolean {
        val config = artifact.phaseContext.config
        if (config.metadataKlib) return true
        val skipInHeaderMode = config.configuration.languageVersionSettings.getFlag(AnalysisFlags.headerMode) &&
                config.configuration.languageVersionSettings.getFlag(AnalysisFlags.headerModeType) == HeaderMode.COMPILATION &&
                !requireIrForHeaderCompilationMode(artifact.frontendOutput)
        return skipInHeaderMode
    }
}
