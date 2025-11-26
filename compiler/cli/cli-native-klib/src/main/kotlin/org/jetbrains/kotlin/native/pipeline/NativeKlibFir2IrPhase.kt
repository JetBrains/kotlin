/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.native.pipeline

import org.jetbrains.kotlin.backend.konan.KonanCompilationException
import org.jetbrains.kotlin.cli.pipeline.PerformanceNotifications
import org.jetbrains.kotlin.cli.pipeline.PipelinePhase
import org.jetbrains.kotlin.native.FirOutput
import org.jetbrains.kotlin.native.fir2Ir

/**
 * Fir2Ir phase for Native klib compilation.
 *
 * This phase converts FIR output to IR.
 */
object NativeKlibFir2IrPhase : PipelinePhase<NativeKlibFrontendArtifact, NativeKlibFir2IrArtifact>(
    name = "NativeKlibFir2IrPhase",
    preActions = setOf(PerformanceNotifications.TranslationToIrStarted),
    postActions = setOf(PerformanceNotifications.TranslationToIrFinished),
) {
    override fun executePhase(input: NativeKlibFrontendArtifact): NativeKlibFir2IrArtifact? {
        val phaseContext = input.phaseContext
        val frontendOutput = input.frontendOutput

        return try {
            val firOutput = FirOutput.Full(frontendOutput)
            val fir2IrOutput = phaseContext.fir2Ir(firOutput)


            NativeKlibFir2IrArtifact(
                phaseContext = phaseContext,
                result = fir2IrOutput.fir2irActualizedResult,
                diagnosticCollector = input.diagnosticCollector,
                fir2IrOutput = fir2IrOutput,
            )
        } catch (e: KonanCompilationException) {
            // Fir2Ir errors are already reported
            null
        }
    }
}
