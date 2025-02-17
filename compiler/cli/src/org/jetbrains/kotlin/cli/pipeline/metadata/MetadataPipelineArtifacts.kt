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
import org.jetbrains.kotlin.config.metadataVersion
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.fir.pipeline.FirResult
import org.jetbrains.kotlin.metadata.builtins.BuiltInsBinaryVersion

data class MetadataFrontendPipelineArtifact(
    override val result: FirResult,
    val configuration: CompilerConfiguration,
    val diagnosticCollector: BaseDiagnosticsCollector,
    val sourceFiles: List<KtSourceFile>,
) : FrontendPipelineArtifact() {
    val metadataVersion: BuiltInsBinaryVersion = configuration.metadataVersion as? BuiltInsBinaryVersion ?: BuiltInsBinaryVersion.INSTANCE
}

data class MetadataSerializationArtifact(
    val outputInfo: OutputInfo?,
    val configuration: CompilerConfiguration,
    val destination: String,
) : PipelineArtifact()
