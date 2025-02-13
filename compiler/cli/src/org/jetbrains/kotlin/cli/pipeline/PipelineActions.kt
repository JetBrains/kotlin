/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.pipeline

import org.jetbrains.kotlin.cli.common.fir.FirDiagnosticsCompilerResultsReporter
import org.jetbrains.kotlin.config.phaser.Action
import org.jetbrains.kotlin.config.phaser.ActionState
import org.jetbrains.kotlin.util.PhaseMeasurementType

abstract class CheckCompilationErrors : Action<PipelineArtifact, PipelineContext> {
    object CheckMessageCollector : CheckCompilationErrors() {
        override fun invoke(
            state: ActionState,
            output: PipelineArtifact,
            c: PipelineContext,
        ) {
            if (c.messageCollector.hasErrors()) {
                throw PipelineStepException()
            }
        }
    }

    object CheckDiagnosticCollector : CheckCompilationErrors() {
        override fun invoke(
            state: ActionState,
            output: PipelineArtifact,
            c: PipelineContext,
        ) {
            if (c.kaptMode) return
            if (c.diagnosticCollector.hasErrors || c.messageCollector.hasErrors()) {
                throw PipelineStepException()
            }
        }

        fun reportDiagnosticsToMessageCollector(c: PipelineContext) {
            FirDiagnosticsCompilerResultsReporter.reportToMessageCollector(
                c.diagnosticCollector, c.messageCollector,
                c.renderDiagnosticInternalName
            )
        }
    }
}

object PerformanceNotifications {
    // frontend
    object AnalysisStarted : AbstractNotification(PhaseMeasurementType.Analysis, start = true)
    object AnalysisFinished : AbstractNotification(PhaseMeasurementType.Analysis, start = false)

    // fir2ir
    object IrGenerationStarted : AbstractNotification(PhaseMeasurementType.IrGeneration, start = true)
    object IrGenerationFinished : AbstractNotification(PhaseMeasurementType.IrGeneration, start = false)

    // backend lowerings
    object IrLoweringStarted : AbstractNotification(PhaseMeasurementType.IrLowering, start = true)
    object IrLoweringFinished : AbstractNotification(PhaseMeasurementType.IrLowering, start = false)

    // backend codegen
    object BackendGenerationStarted : AbstractNotification(PhaseMeasurementType.BackendGeneration, start = true)
    object BackendGenerationFinished : AbstractNotification(PhaseMeasurementType.BackendGeneration, start = false)

    sealed class AbstractNotification(
        val phaseType: PhaseMeasurementType,
        val start: Boolean,
    ) : Action<PipelineArtifact, PipelineContext> {
        override fun invoke(
            state: ActionState,
            input: PipelineArtifact,
            c: PipelineContext,
        ) {
            if (start) {
                c.performanceManager.notifyPhaseStarted(phaseType)
            } else {
                c.performanceManager.notifyPhaseFinished(phaseType)
            }
        }
    }
}
