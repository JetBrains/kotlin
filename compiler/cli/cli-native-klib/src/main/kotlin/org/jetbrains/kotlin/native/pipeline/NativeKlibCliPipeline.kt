/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.native.pipeline

import org.jetbrains.kotlin.backend.common.phaser.then
import org.jetbrains.kotlin.cli.common.arguments.K2NativeKlibCompilerArguments
import org.jetbrains.kotlin.cli.pipeline.AbstractCliPipeline
import org.jetbrains.kotlin.cli.pipeline.ArgumentsPipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.PipelineContext
import org.jetbrains.kotlin.config.phaser.CompilerPhase
import org.jetbrains.kotlin.util.PerformanceManager

/**
 * CLI pipeline for Native klib compilation.
 *
 * This pipeline compiles Kotlin sources into Native klib format without requiring
 * the full Native backend infrastructure from kotlin-native/backend.native.
 *
 * The pipeline consists of the following phases:
 * 1. Configuration - Configuration setup and library resolution
 * 2. Context Creation - Creates the Native-specific phase context
 * 3. Frontend - FIR frontend compilation
 * 4. Fir2Ir - FIR to IR conversion
 * 5. Serializer - IR serialization to klib
 */
class NativeKlibCliPipeline(
    override val defaultPerformanceManager: PerformanceManager
) : AbstractCliPipeline<K2NativeKlibCompilerArguments>() {

    override fun createCompoundPhase(
        arguments: K2NativeKlibCompilerArguments
    ): CompilerPhase<PipelineContext, ArgumentsPipelineArtifact<K2NativeKlibCompilerArguments>, NativeKlibSerializerArtifact> {
        return NativeKlibConfigurationPhase then
                NativeKlibContextCreationPhase then
                NativeKlibFrontendPhase then
                NativeKlibFir2IrPhase then
                NativeKlibSerializerPhase
    }
}
