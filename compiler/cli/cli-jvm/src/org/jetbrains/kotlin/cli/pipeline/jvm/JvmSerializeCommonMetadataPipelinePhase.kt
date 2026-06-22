/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.pipeline.jvm

import org.jetbrains.kotlin.cli.pipeline.CheckCompilationErrors
import org.jetbrains.kotlin.cli.pipeline.PerformanceNotifications
import org.jetbrains.kotlin.cli.pipeline.PipelinePhase
import org.jetbrains.kotlin.cli.pipeline.metadata.MetadataFrontendPipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.metadata.MetadataKlibFileWriterPhase
import org.jetbrains.kotlin.cli.pipeline.metadata.MetadataKlibInMemorySerializerPhase
import org.jetbrains.kotlin.config.commonFragmentsOutputDir
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.pipeline.AllModulesFrontendOutput

object JvmSerializeCommonMetadataPipelinePhase : PipelinePhase<JvmFrontendPipelineArtifact, JvmFrontendPipelineArtifact>(
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
        val outputDir = configuration.commonFragmentsOutputDir ?: return
        val commonFragmentOutputs = input.frontendOutput.outputs.dropLast(1)
        if (commonFragmentOutputs.isEmpty()) return

        for (output in commonFragmentOutputs) {
            val inputForPhase = MetadataFrontendPipelineArtifact(
                AllModulesFrontendOutput(listOf(output)),
                configuration = configuration,
                sourceFiles = input.sourceFiles,
                isForIncrementalCompilation = true
            )
            val metadataInMemory = MetadataKlibInMemorySerializerPhase.executePhase(inputForPhase)
            MetadataKlibFileWriterPhase.writeToDisc(
                metadataInMemory,
                outputDir.resolve(output.session.moduleData.name.asStringStripSpecialMarkers())
            )
        }
    }
}
