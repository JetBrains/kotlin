/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.native.pipeline

import org.jetbrains.kotlin.cli.pipeline.CheckCompilationErrors
import org.jetbrains.kotlin.cli.pipeline.PerformanceNotifications
import org.jetbrains.kotlin.cli.pipeline.PipelinePhase
import org.jetbrains.kotlin.native.FirOutput
import org.jetbrains.kotlin.native.NativePhaseContext
import org.jetbrains.kotlin.native.createNativeKlibConfig
import org.jetbrains.kotlin.native.firFrontend

object NativeFrontendPhase : PipelinePhase<NativeConfigurationArtifact, NativeFrontendArtifact>(
    name = "NativeFrontendPhase",
    preActions = setOf(PerformanceNotifications.AnalysisStarted),
    postActions = setOf(PerformanceNotifications.AnalysisFinished, CheckCompilationErrors.CheckDiagnosticCollector)
) {
    override fun executePhase(input: NativeConfigurationArtifact): NativeFrontendArtifact? {
        val environment = input.environment

        val configuration = input.configuration
        val config = createNativeKlibConfig(configuration)
        val phaseContext = NativePhaseContext(config)

        return when (val firOutput = phaseContext.firFrontend(environment)) {
            FirOutput.ShouldNotGenerateCode -> null
            is FirOutput.Full -> NativeFrontendArtifact(
                firOutput.firResult,
                configuration = configuration,
                environment = environment,
                diagnosticCollector = input.diagnosticCollector,
                phaseContext = phaseContext,
            )
        }
    }
}
