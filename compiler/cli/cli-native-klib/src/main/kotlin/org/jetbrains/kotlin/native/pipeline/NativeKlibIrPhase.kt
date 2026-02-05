/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.native.pipeline

import org.jetbrains.kotlin.backend.common.serialization.SerializerOutput
import org.jetbrains.kotlin.cli.pipeline.CheckCompilationErrors
import org.jetbrains.kotlin.cli.pipeline.PerformanceNotifications
import org.jetbrains.kotlin.cli.pipeline.PipelinePhase
import org.jetbrains.kotlin.konan.config.konanGeneratedHeaderKlibPath
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.native.FirSerializerInput
import org.jetbrains.kotlin.native.KlibWriterInput
import org.jetbrains.kotlin.native.NativeFirstStagePhaseContext
import org.jetbrains.kotlin.native.firSerializerBase
import org.jetbrains.kotlin.native.writeKlib

/**
 * Serialization phase for native klib compilation.
 * Serializes IR to klib format and writes it to disk.
 * Also handles header klib generation if configured.
 */
object NativeIrSerializationPhase : PipelinePhase<NativeFir2IrArtifact, NativeSerializationArtifact>(
    name = "NativeIrSerializationPhase",
    preActions = setOf(PerformanceNotifications.IrSerializationStarted),
    postActions = setOf(PerformanceNotifications.IrSerializationFinished, CheckCompilationErrors.CheckDiagnosticCollector)
) {
    override fun executePhase(input: NativeFir2IrArtifact): NativeSerializationArtifact? {
        val (fir2IrOutput, configuration, _, diagnosticCollector, phaseContext) = input
        val headerKlibPath = configuration.konanGeneratedHeaderKlibPath?.removeSuffix(".klib")
        val outputKlibPath = phaseContext.config.outputPath
        if (!headerKlibPath.isNullOrEmpty()) {
            val headerKlib = phaseContext.fir2IrSerializer(
                FirSerializerInput(fir2IrOutput, produceHeaderKlib = true)
            )
            val headerKlibInput = KlibWriterInput(headerKlib, headerKlibPath, produceHeaderKlib = true)
            phaseContext.writeKlib(headerKlibInput)

            // Don't overwrite the header klib with the full klib and stop compilation here.
            // By providing the same path for both regular output and header klib we can skip emitting the full klib.
            if (File(outputKlibPath).canonicalPath == File(headerKlibPath).canonicalPath) {
                return null
            }
        }
        val serializerOutput = phaseContext.fir2IrSerializer(
            FirSerializerInput(fir2IrOutput, produceHeaderKlib = false)
        )
        return NativeSerializationArtifact(
            serializerOutput = serializerOutput,
            configuration = configuration,
            diagnosticsCollector = diagnosticCollector,
            phaseContext = phaseContext,
        )
    }

    private fun NativeFirstStagePhaseContext.fir2IrSerializer(input: FirSerializerInput): SerializerOutput {
        return firSerializerBase(input.firToIrOutput.frontendOutput, input.firToIrOutput, produceHeaderKlib = input.produceHeaderKlib)
    }
}

object NativeKlibWritingPhase : PipelinePhase<NativeSerializationArtifact, NativeKlibSerializedArtifact>(
    name = "NativeKlibWritingPhase",
    preActions = setOf(PerformanceNotifications.KlibWritingStarted),
    postActions = setOf(PerformanceNotifications.KlibWritingFinished, CheckCompilationErrors.CheckDiagnosticCollector)
) {
    override fun executePhase(input: NativeSerializationArtifact): NativeKlibSerializedArtifact {
        val (serializerOutput, configuration, diagnosticCollector, phaseContext) = input
        val outputKlibPath = phaseContext.config.outputPath
        phaseContext.writeKlib(
            KlibWriterInput(serializerOutput, outputKlibPath, produceHeaderKlib = false)
        )
        return NativeKlibSerializedArtifact(
            outputKlibPath = outputKlibPath,
            configuration = configuration,
            diagnosticsCollector = diagnosticCollector,
        )
    }
}

