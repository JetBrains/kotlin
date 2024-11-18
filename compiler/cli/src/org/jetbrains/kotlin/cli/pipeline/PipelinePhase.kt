/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.pipeline

import org.jetbrains.kotlin.cli.common.CommonCompilerPerformanceManager
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.config.LoggingContext
import org.jetbrains.kotlin.config.phaser.Action
import org.jetbrains.kotlin.config.phaser.PhaseConfigurationService
import org.jetbrains.kotlin.config.phaser.PhaserState
import org.jetbrains.kotlin.config.phaser.SimpleNamedCompilerPhase
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.utils.addToStdlib.shouldNotBeCalled

class PipelineContext(
    val messageCollector: MessageCollector,
    val diagnosticCollector: BaseDiagnosticsCollector,
    val performanceManager: CommonCompilerPerformanceManager,
    val renderDiagnosticInternalName: Boolean,
    val kaptMode: Boolean
) : LoggingContext {
    override var inVerbosePhase: Boolean = false
}

abstract class PipelinePhase<I : PipelineArtifact, O : PipelineArtifact>(
    name: String,
    preActions: Set<Action<I, PipelineContext>> = emptySet(),
    postActions: Set<Action<Pair<I, O>, PipelineContext>> = emptySet(),
) : SimpleNamedCompilerPhase<PipelineContext, I, O>(
    name = name,
    preactions = preActions,
    postactions = postActions
) {
    override fun phaseBody(context: PipelineContext, input: I): O {
        return executePhase(input) ?: throw PipelineStepException()
    }

    abstract fun executePhase(input: I): O?

    override fun outputIfNotEnabled(
        phaseConfig: PhaseConfigurationService,
        phaserState: PhaserState<I>,
        context: PipelineContext,
        input: I,
    ): O {
        shouldNotBeCalled()
    }
}

class PipelineStepException(val definitelyCompilerError: Boolean = false) : RuntimeException()
class SuccessfulPipelineExecutionException : RuntimeException()
