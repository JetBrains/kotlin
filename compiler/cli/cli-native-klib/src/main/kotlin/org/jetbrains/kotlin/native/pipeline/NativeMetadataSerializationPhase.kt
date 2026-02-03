/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.native.pipeline

import org.jetbrains.kotlin.backend.common.serialization.SerializerOutput
import org.jetbrains.kotlin.backend.konan.driver.PhaseContext
import org.jetbrains.kotlin.cli.pipeline.CheckCompilationErrors
import org.jetbrains.kotlin.cli.pipeline.PerformanceNotifications
import org.jetbrains.kotlin.cli.pipeline.PipelinePhase
import org.jetbrains.kotlin.native.FirOutput
import org.jetbrains.kotlin.native.firSerializerBase

object NativeMetadataSerializationPhase : PipelinePhase<NativeFrontendArtifact, NativeSerializationArtifact>(
    name = "NativeMetadataSerializationPhase",
    preActions = setOf(PerformanceNotifications.KlibWritingStarted),
    postActions = setOf(PerformanceNotifications.KlibWritingFinished, CheckCompilationErrors.CheckDiagnosticCollector)
) {
    override fun executePhase(input: NativeFrontendArtifact): NativeSerializationArtifact? {
        val firOutput = input.frontendOutput
        val configuration = input.configuration
        val phaseContext = input.phaseContext

        val serializerOutput = phaseContext.firSerializer(FirOutput.Full(firOutput))
            ?: return null
        return NativeSerializationArtifact(
            serializerOutput = serializerOutput,
            configuration = configuration,
            diagnosticCollector = input.diagnosticCollector,
            phaseContext = phaseContext,
        )
    }

    fun PhaseContext.firSerializer(input: FirOutput): SerializerOutput? = when (input) {
        !is FirOutput.Full -> null
        else -> firSerializerBase(input.firResult, null)
    }
}