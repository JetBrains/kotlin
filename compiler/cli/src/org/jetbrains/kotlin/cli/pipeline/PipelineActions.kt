/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.pipeline

import org.jetbrains.kotlin.cli.common.fir.FirDiagnosticsCompilerResultsReporter
import org.jetbrains.kotlin.config.phaser.Action
import org.jetbrains.kotlin.config.phaser.ActionState
import org.jetbrains.kotlin.util.CommonCompilerPerformanceManager

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
    object AnalysisStarted : AbstractNotification(CommonCompilerPerformanceManager::notifyAnalysisStarted)
    object AnalysisFinished : AbstractNotification(CommonCompilerPerformanceManager::notifyAnalysisFinished)

    // fir2ir
    object IrTranslationStarted : AbstractNotification(CommonCompilerPerformanceManager::notifyIRTranslationStarted)
    object IrTranslationFinished : AbstractNotification(CommonCompilerPerformanceManager::notifyIRTranslationFinished)

    // backend lowerings
    object IrLoweringStarted : AbstractNotification(CommonCompilerPerformanceManager::notifyIRLoweringStarted)
    object IrLoweringFinished : AbstractNotification(CommonCompilerPerformanceManager::notifyIRLoweringFinished)

    // backend codegen
    object IrGenerationStarted : AbstractNotification(CommonCompilerPerformanceManager::notifyIRGenerationStarted)
    object IrGenerationFinished : AbstractNotification(CommonCompilerPerformanceManager::notifyIRGenerationFinished)

    // whole backend
    object GenerationStarted : AbstractNotification(CommonCompilerPerformanceManager::notifyGenerationStarted)
    object GenerationFinished : AbstractNotification(CommonCompilerPerformanceManager::notifyGenerationFinished)

    sealed class AbstractNotification(
        val notify: CommonCompilerPerformanceManager.() -> Unit
    ) : Action<PipelineArtifact, PipelineContext> {
        override fun invoke(
            state: ActionState,
            input: PipelineArtifact,
            c: PipelineContext,
        ) {
            c.performanceManager.notify()
        }
    }
}
