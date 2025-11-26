/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.native.pipeline

import org.jetbrains.kotlin.backend.konan.KonanCompilationException
import org.jetbrains.kotlin.cli.pipeline.CheckCompilationErrors
import org.jetbrains.kotlin.cli.pipeline.PerformanceNotifications
import org.jetbrains.kotlin.cli.pipeline.PipelinePhase
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.native.FirOutput
import org.jetbrains.kotlin.native.firFrontendWithLightTree

/**
 * Frontend phase for Native klib compilation.
 *
 * This phase runs the FIR frontend to analyze Kotlin sources and produce FIR output.
 */
object NativeKlibFrontendPhase : PipelinePhase<NativeKlibConfigurationArtifact, NativeKlibFrontendArtifact>(
    name = "NativeKlibFrontendPhase",
    postActions = setOf(PerformanceNotifications.AnalysisFinished, CheckCompilationErrors.CheckDiagnosticCollector),
) {
    override fun executePhase(input: NativeKlibConfigurationArtifact): NativeKlibFrontendArtifact? {
        val phaseContext = input.phaseContext
        val configuration = input.configuration
        val environment = input.environment

        return try {
            val firOutput = phaseContext.firFrontendWithLightTree(environment)

            when (firOutput) {
                is FirOutput.Full -> NativeKlibFrontendArtifact(
                    phaseContext = phaseContext,
                    frontendOutput = firOutput.firResult,
                    configuration = configuration,
                    diagnosticCollector = input.diagnosticCollector,
                )
                else -> null
            }
        } catch (e: KonanCompilationException) {
            // Frontend errors are already reported
            null
        }
    }
}
