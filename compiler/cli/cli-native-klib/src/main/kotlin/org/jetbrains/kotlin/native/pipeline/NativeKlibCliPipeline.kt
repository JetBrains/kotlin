/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.native.pipeline

import org.jetbrains.kotlin.backend.common.phaser.then
import org.jetbrains.kotlin.cli.common.arguments.K2NativeCompilerArguments
import org.jetbrains.kotlin.cli.pipeline.AbstractCliPipeline
import org.jetbrains.kotlin.cli.pipeline.ArgumentsPipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.PipelineContext
import org.jetbrains.kotlin.config.phaser.CompilerPhase
import org.jetbrains.kotlin.util.PerformanceManager

class NativeKlibCliPipeline(
    override val defaultPerformanceManager: PerformanceManager,
    override val isNativeOneStage: Boolean,
) : AbstractCliPipeline<K2NativeCompilerArguments>() {

    override fun createCompoundPhase(
        arguments: K2NativeCompilerArguments
    ): CompilerPhase<PipelineContext, ArgumentsPipelineArtifact<K2NativeCompilerArguments>, *> {
        return NativeConfigurationPhase then
                NativeEnvironmentPhase then
                NativeFrontendPhase then
                if (arguments.metadataKlib) {
                    NativeMetadataSerializationPhase then
                    NativeKlibWritingPhase
                } else {
                    NativeFir2IrPhase then
                    NativePreSerializationPhase then
                    NativeIrSerializationPhase then
                    NativeKlibWritingPhase
                }
    }
}
