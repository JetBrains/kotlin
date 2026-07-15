/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.pipeline.jvm.metadata

import org.jetbrains.kotlin.cli.pipeline.CheckCompilationErrors
import org.jetbrains.kotlin.cli.pipeline.PerformanceNotifications
import org.jetbrains.kotlin.cli.pipeline.PipelinePhase
import org.jetbrains.kotlin.cli.pipeline.jvm.JvmFrontendPipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.metadata.MetadataFrontendPipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.metadata.MetadataKlibInMemorySerializerPhase
import org.jetbrains.kotlin.config.icMetadataTracker
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.pipeline.AllModulesFrontendOutput
import java.io.File

internal object JvmSerializeCommonMetadataPipelinePhase : PipelinePhase<JvmFrontendPipelineArtifact, JvmFrontendPipelineArtifact>(
    name = "JvmSerializeCommonMetadataPipelinePhase",
    preActions = setOf(PerformanceNotifications.KlibWritingStarted),
    postActions = setOf(PerformanceNotifications.KlibWritingFinished, CheckCompilationErrors.CheckDiagnosticCollector)
) {
    override fun executePhase(input: JvmFrontendPipelineArtifact): JvmFrontendPipelineArtifact {
        serializeFragmentsIfNeeded(input)
        return input
    }

    private fun serializeFragmentsIfNeeded(input: JvmFrontendPipelineArtifact) {
        val configuration = input.configuration
        val commonFragmentOutputs = input.frontendOutput.outputs.dropLast(1)
        if (commonFragmentOutputs.isEmpty()) return

        for (output in commonFragmentOutputs) {
            val inputForPhase = MetadataFrontendPipelineArtifact(
                AllModulesFrontendOutput(listOf(output)),
                configuration = configuration,
                sourceFiles = input.sourceFiles,
            )

            val moduleName = output.session.moduleData.name.asStringStripSpecialMarkers()
            val serializedMetadata = MetadataKlibInMemorySerializerPhase.executePhase(inputForPhase)

            configuration.icMetadataTracker?.let { metadataTracker ->
                serializedMetadata.firMetadata.fragments.flatten().forEach { firFile ->
                    // Fragments without a source path come from content generated for compiler plugins. We intentionally
                    // don't track them: IC always marks such generated files as dirty before compilation, so their
                    // metadata is regenerated on every round and never needs to be restored from the cache.
                    firFile.path?.let { metadataTracker.report(moduleName, File(it), firFile.content) }
                }
            }
        }
    }
}
