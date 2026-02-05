/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.pipeline.metadata

import org.jetbrains.kotlin.KtSourceFile
import org.jetbrains.kotlin.cli.metadata.AbstractMetadataSerializer.OutputInfo
import org.jetbrains.kotlin.cli.pipeline.FrontendPipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.PipelineArtifact
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.fir.pipeline.AllModulesFrontendOutput
import org.jetbrains.kotlin.library.SerializedMetadata

data class MetadataFrontendPipelineArtifact(
    override val frontendOutput: AllModulesFrontendOutput,
    override val configuration: CompilerConfiguration,
    override val diagnosticsCollector: BaseDiagnosticsCollector,
    val sourceFiles: List<KtSourceFile>,
) : FrontendPipelineArtifact() {
    @CliPipelineInternals(OPT_IN_MESSAGE)
    override fun withCompilerConfiguration(newConfiguration: CompilerConfiguration): MetadataFrontendPipelineArtifact {
        return copy(configuration = newConfiguration)
    }

    override fun withNewFrontendOutputImpl(newFrontendOutput: AllModulesFrontendOutput): FrontendPipelineArtifact {
        return copy(frontendOutput = newFrontendOutput)
    }
}

data class MetadataInMemorySerializationArtifact(
    val metadata: SerializedMetadata,
    override val configuration: CompilerConfiguration,
) : PipelineArtifact() {
    @CliPipelineInternals(OPT_IN_MESSAGE)
    override fun withCompilerConfiguration(newConfiguration: CompilerConfiguration): MetadataInMemorySerializationArtifact {
        return copy(configuration = newConfiguration)
    }
}

data class MetadataSerializationArtifact(
    val outputInfo: OutputInfo?,
    override val configuration: CompilerConfiguration,
    val destination: String,
) : PipelineArtifact() {
    @CliPipelineInternals(OPT_IN_MESSAGE)
    override fun withCompilerConfiguration(newConfiguration: CompilerConfiguration): MetadataSerializationArtifact {
        return copy(configuration = newConfiguration)
    }
}
