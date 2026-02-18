/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.native.pipeline

import org.jetbrains.kotlin.backend.common.phaser.PhaseEngine
import org.jetbrains.kotlin.backend.konan.lower.SpecialBackendChecksTraversal
import org.jetbrains.kotlin.cli.pipeline.CheckCompilationErrors
import org.jetbrains.kotlin.cli.pipeline.PerformanceNotifications
import org.jetbrains.kotlin.cli.pipeline.PipelinePhase
import org.jetbrains.kotlin.config.phaseConfig
import org.jetbrains.kotlin.config.phaser.PhaseConfig
import org.jetbrains.kotlin.config.phaser.PhaserState
import org.jetbrains.kotlin.native.fir2Ir
import org.jetbrains.kotlin.native.runPreSerializationLowerings

object NativeFir2IrPhase : PipelinePhase<NativeFrontendArtifact, NativeFir2IrArtifact>(
    name = "NativeFir2IrPhase",
    preActions = setOf(PerformanceNotifications.TranslationToIrStarted),
    postActions = setOf(PerformanceNotifications.TranslationToIrFinished, CheckCompilationErrors.CheckDiagnosticCollector)
) {
    override fun executePhase(input: NativeFrontendArtifact): NativeFir2IrArtifact {
        val (frontendOutput, configuration, environment, phaseContext) = input
        val fir2IrResult = phaseContext.fir2Ir(frontendOutput)
        SpecialBackendChecksTraversal(
            phaseContext,
            fir2IrResult.symbols,
            fir2IrResult.fir2irActualizedResult.irBuiltIns,
        ).lower(fir2IrResult.fir2irActualizedResult.irModuleFragment)
        return NativeFir2IrArtifact(
            fir2IrOutput = fir2IrResult,
            configuration = configuration,
            environment = environment,
            phaseContext = phaseContext,
        )
    }
}

object NativePreSerializationPhase : PipelinePhase<NativeFir2IrArtifact, NativeFir2IrArtifact>(
    name = "NativePreSerializationPhase",
    preActions = setOf(PerformanceNotifications.IrPreLoweringStarted),
    postActions = setOf(PerformanceNotifications.IrPreLoweringFinished, CheckCompilationErrors.CheckDiagnosticCollector)
) {
    override fun executePhase(input: NativeFir2IrArtifact): NativeFir2IrArtifact {
        val (fir2IrOutput, configuration, environment, phaseContext) = input
        val phaseConfig = configuration.phaseConfig ?: PhaseConfig()
        val phaserState = PhaserState()
        val engine = PhaseEngine(phaseConfig, phaserState, phaseContext)
        val loweredResult = engine.runPreSerializationLowerings(fir2IrOutput, environment)
        return NativeFir2IrArtifact(
            fir2IrOutput = loweredResult,
            configuration = configuration,
            environment = environment,
            phaseContext = phaseContext,
        )
    }
}
