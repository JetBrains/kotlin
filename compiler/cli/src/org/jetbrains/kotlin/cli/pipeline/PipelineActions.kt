/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.pipeline

import org.jetbrains.kotlin.cli.common.CommonCompilerPerformanceManager
import org.jetbrains.kotlin.cli.common.fir.FirDiagnosticsCompilerResultsReporter
import org.jetbrains.kotlin.config.phaser.Action
import org.jetbrains.kotlin.config.phaser.ActionState

abstract class CheckCompilationErrors : Action<Pair<PipelineArtifact, PipelineArtifact>, PipelineContext> {
    object CheckMessageCollector : CheckCompilationErrors() {
        override fun invoke(
            state: ActionState,
            artifacts: Pair<PipelineArtifact, PipelineArtifact>,
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
            artifacts: Pair<PipelineArtifact, PipelineArtifact>,
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
    object AnalysisStarted : AbstractStartedNotification(CommonCompilerPerformanceManager::notifyAnalysisStarted)
    object AnalysisFinished : AbstractFinishedNotification(CommonCompilerPerformanceManager::notifyAnalysisFinished)

    object IrTranslationStarted : AbstractStartedNotification(CommonCompilerPerformanceManager::notifyIRTranslationStarted)
    object IrTranslationFinished : AbstractFinishedNotification(CommonCompilerPerformanceManager::notifyIRTranslationFinished)

    object IrLoweringsStarted : AbstractStartedNotification(CommonCompilerPerformanceManager::notifyIRLoweringStarted)
    object IrLoweringsFinished : AbstractFinishedNotification(CommonCompilerPerformanceManager::notifyIRLoweringFinished)

    object GenerationStarted : AbstractStartedNotification(CommonCompilerPerformanceManager::notifyGenerationStarted)
    object GenerationFinished : AbstractFinishedNotification(CommonCompilerPerformanceManager::notifyGenerationFinished)

    sealed class AbstractStartedNotification(
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

    sealed class AbstractFinishedNotification(
        val notify: CommonCompilerPerformanceManager.() -> Unit
    ) : Action<Pair<PipelineArtifact, PipelineArtifact>, PipelineContext> {
        override fun invoke(
            state: ActionState,
            artifacts: Pair<PipelineArtifact, PipelineArtifact>,
            c: PipelineContext,
        ) {
            c.performanceManager.notify()
        }
    }
}
