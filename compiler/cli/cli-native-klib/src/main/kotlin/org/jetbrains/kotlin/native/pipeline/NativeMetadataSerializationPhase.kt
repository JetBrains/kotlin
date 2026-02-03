/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.native.pipeline

import org.jetbrains.kotlin.cli.pipeline.CheckCompilationErrors
import org.jetbrains.kotlin.cli.pipeline.PerformanceNotifications
import org.jetbrains.kotlin.cli.pipeline.PipelinePhase
import org.jetbrains.kotlin.native.firSerializerBase

object NativeMetadataSerializationPhase : PipelinePhase<NativeFrontendArtifact, NativeSerializationArtifact>(
    name = "NativeMetadataSerializationPhase",
    preActions = setOf(PerformanceNotifications.KlibWritingStarted),
    postActions = setOf(PerformanceNotifications.KlibWritingFinished, CheckCompilationErrors.CheckDiagnosticCollector)
) {
    override fun executePhase(input: NativeFrontendArtifact): NativeSerializationArtifact {
        val (frontendOutput, configuration, _, diagnosticCollector, phaseContext) = input

        val serializerOutput = phaseContext.firSerializerBase(frontendOutput, fir2IrOutput = null)
        return NativeSerializationArtifact(
            serializerOutput = serializerOutput,
            configuration = configuration,
            diagnosticCollector = diagnosticCollector,
            phaseContext = phaseContext,
        )
    }
}