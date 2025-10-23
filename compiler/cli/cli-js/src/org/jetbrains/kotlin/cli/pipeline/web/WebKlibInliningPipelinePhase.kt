/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.pipeline.web

import org.jetbrains.kotlin.backend.common.phaser.PhaseEngine
import org.jetbrains.kotlin.cli.common.runPreSerializationLoweringPhases
import org.jetbrains.kotlin.cli.pipeline.PerformanceNotifications
import org.jetbrains.kotlin.cli.pipeline.PipelinePhase
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.incrementalCompilation
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.config.phaseConfig
import org.jetbrains.kotlin.config.phaser.PhaserState
import org.jetbrains.kotlin.fir.pipeline.Fir2IrActualizedResult
import org.jetbrains.kotlin.fir.pipeline.Fir2KlibMetadataSerializer
import org.jetbrains.kotlin.backend.wasm.WasmPreSerializationLoweringContext
import org.jetbrains.kotlin.backend.wasm.wasmLoweringsOfTheFirstPhase
import org.jetbrains.kotlin.config.messageCollector
import org.jetbrains.kotlin.diagnostics.DiagnosticReporterFactory
import org.jetbrains.kotlin.diagnostics.impl.deduplicating
import org.jetbrains.kotlin.ir.KtDiagnosticReporterWithImplicitIrBasedContext
import org.jetbrains.kotlin.ir.backend.js.JsPreSerializationLoweringContext
import org.jetbrains.kotlin.ir.backend.js.ModulesStructure
import org.jetbrains.kotlin.ir.backend.js.jsLoweringsOfTheFirstPhase
import org.jetbrains.kotlin.ir.backend.js.shouldGoToNextIcRound
import org.jetbrains.kotlin.js.config.wasmCompilation
import org.jetbrains.kotlin.progress.IncrementalNextRoundException

object WebKlibInliningPipelinePhase : PipelinePhase<JsFir2IrPipelineArtifact, JsFir2IrPipelineArtifact>(
    name = "WebKlibInliningPipelinePhase",
    preActions = setOf(PerformanceNotifications.IrPreLoweringStarted),
    postActions = setOf(PerformanceNotifications.IrPreLoweringFinished),
) {
    override fun executePhase(input: JsFir2IrPipelineArtifact): JsFir2IrPipelineArtifact {
        val (fir2IrResult, firOutput, configuration, _, moduleStructure) = input
        processIncrementalCompilationRoundIfNeeded(configuration, moduleStructure, firOutput, fir2IrResult)
        val diagnosticCollector = DiagnosticReporterFactory.createPendingReporter(configuration.messageCollector)
        val irDiagnosticReporter = KtDiagnosticReporterWithImplicitIrBasedContext(
            diagnosticCollector.deduplicating(),
            configuration.languageVersionSettings
        )

        val transformedResult = if (configuration.wasmCompilation) {
            PhaseEngine(
                configuration.phaseConfig!!,
                PhaserState(),
                WasmPreSerializationLoweringContext(fir2IrResult.irBuiltIns, configuration, irDiagnosticReporter),
            ).runPreSerializationLoweringPhases(fir2IrResult, wasmLoweringsOfTheFirstPhase(configuration.languageVersionSettings))
        } else {
            PhaseEngine(
                configuration.phaseConfig!!,
                PhaserState(),
                JsPreSerializationLoweringContext(fir2IrResult.irBuiltIns, configuration, irDiagnosticReporter),
            ).runPreSerializationLoweringPhases(fir2IrResult, jsLoweringsOfTheFirstPhase(configuration.languageVersionSettings))
        }

        return input.copy(result = transformedResult, diagnosticCollector = diagnosticCollector)
    }

    private fun processIncrementalCompilationRoundIfNeeded(
        configuration: CompilerConfiguration,
        moduleStructure: ModulesStructure,
        firOutput: AnalyzedFirOutput,
        fir2IrResult: Fir2IrActualizedResult,
    ) {
        if (!configuration.incrementalCompilation) return
        // TODO: During checking the next round, fir serializer may throw an exception, e.g.
        //      during annotation serialization when it cannot find the removed constant
        //      (see ConstantValueUtils.kt:convertToConstantValues())
        //  This happens because we check the next round before compilation errors.
        //  Test reproducer:  testFileWithConstantRemoved
        //  Issue: https://youtrack.jetbrains.com/issue/KT-58824/
        val shouldGoToNextIcRound = shouldGoToNextIcRound(moduleStructure.compilerConfiguration) {
            Fir2KlibMetadataSerializer(
                moduleStructure.compilerConfiguration,
                firOutput.output,
                fir2IrResult,
                exportKDoc = false,
                produceHeaderKlib = false,
            )
        }
        if (shouldGoToNextIcRound) {
            // TODO (KT-73991): investigate the need in this hack
            throw IncrementalNextRoundException()
        }
    }
}
