/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.pipeline

import org.jetbrains.kotlin.cli.common.CommonCompilerPerformanceManager
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.config.LoggingContext
import org.jetbrains.kotlin.config.phaser.Action
import org.jetbrains.kotlin.config.phaser.ActionState
import org.jetbrains.kotlin.config.phaser.PhaseConfigurationService
import org.jetbrains.kotlin.config.phaser.PhaserState
import org.jetbrains.kotlin.config.phaser.SimpleNamedCompilerPhase
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.utils.addToStdlib.shouldNotBeCalled

/**
 * [PipelineContext] contains the information which can be used by pre- and post-actions of pipeline phases to report
 *   some information regarding the executed phase.
 * [PipelinePhase] itself could be run without the context
 */
class PipelineContext(
    val messageCollector: MessageCollector,
    val diagnosticCollector: BaseDiagnosticsCollector,
    val performanceManager: CommonCompilerPerformanceManager,
    val renderDiagnosticInternalName: Boolean,
    val kaptMode: Boolean
) : LoggingContext {
    override var inVerbosePhase: Boolean = false
}

/**
 * This class is the main abstraction for the phases of the phased CLI
 * Each phase represents a step of the pipeline, like
 * - fill the [org.jetbrains.kotlin.config.CompilerConfiguration] from arguments
 * - run frontend
 * - run fir2ir and actualizer
 * - serialize klib
 * - run backend
 *
 * These phases are expected to be isolated, and the only way for them to pass information from one to another
 *   is input/output artifacts
 *
 * These phases are built over [org.jetbrains.kotlin.config.phaser.CompilerPhase] infrastructure, and the CLI uses it to make
 *   a compound phases which consists of several pipeline steps. But also these phases have other usages, like test infrastructure,
 *   which manually calls some steps. Because of that, the main method of [PipelinePhase] ([executePhase]) doesn't contain the [context]
 *   parameter, which is supposed to be used only in CLI pipeline.
 *
 * To control the execution of the pipeline, the [PipelineStepException] is used. Throwing it stops the pipeline
 *
 * [preActions] and [postActions] are callbacks which might be used for several purposes:
 * - callbacks before and after some stages (e.g. to notify the performance manager)
 * - checks that input/output artifacts are consistent (e.g. to check that there no compiler errors were reported). In this case these
 *   actions also might throw [PipelineStepException] to stop the pipeline
 */
abstract class PipelinePhase<I : PipelineArtifact, O : PipelineArtifact>(
    name: String,
    preActions: Set<Action<I, PipelineContext>> = emptySet(),
    postActions: Set<Action<O, PipelineContext>> = emptySet(),
) : SimpleNamedCompilerPhase<PipelineContext, I, O>(
    name = name,
    preactions = preActions,
    postactions = postActions.mapTo(mutableSetOf()) { it.toPostAction() }
) {
    final override fun phaseBody(context: PipelineContext, input: I): O {
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

/**
 * If [definitelyCompilationError] set to `true` the [AbstractCliPipeline] will
 * return [org.jetbrains.kotlin.cli.common.ExitCode.COMPILATION_ERROR] regardless if there
 * are any errors in the message collector or not
 */
class PipelineStepException(val definitelyCompilationError: Boolean = false) : RuntimeException()
class SuccessfulPipelineExecutionException : RuntimeException()

private fun <Input, Output, Context> Action<Output, Context>.toPostAction(): Action<Pair<Input, Output>, Context> {
    return { state: ActionState, inputOutput: Pair<Input, Output>, context: Context ->
        this.invoke(state, inputOutput.second, context)
    }
}
