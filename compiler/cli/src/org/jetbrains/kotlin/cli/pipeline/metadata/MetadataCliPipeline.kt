/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.pipeline.metadata

import org.jetbrains.kotlin.backend.common.phaser.then
import org.jetbrains.kotlin.cli.common.arguments.K2MetadataCompilerArguments
import org.jetbrains.kotlin.cli.pipeline.AbstractCliPipeline
import org.jetbrains.kotlin.cli.pipeline.ArgumentsPipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.PipelineContext
import org.jetbrains.kotlin.cli.pipeline.PipelinePhase
import org.jetbrains.kotlin.config.phaser.CompilerPhase
import org.jetbrains.kotlin.util.PerformanceManager

class MetadataCliPipeline(override val defaultPerformanceManager: PerformanceManager) : AbstractCliPipeline<K2MetadataCompilerArguments>() {
    override fun createCompoundPhase(arguments: K2MetadataCompilerArguments): CompilerPhase<PipelineContext, ArgumentsPipelineArtifact<K2MetadataCompilerArguments>, *> {
        return MetadataConfigurationPipelinePhase then
                MetadataFrontendPipelinePhase then
                serializerPhase(arguments)
    }

    private fun serializerPhase(
        arguments: K2MetadataCompilerArguments
    ): PipelinePhase<MetadataFrontendPipelineArtifact, MetadataSerializationArtifact> = when {
        arguments.legacyMetadataJar -> MetadataLegacySerializerPhase
        else -> MetadataKlibSerializerPhase
    }
}
