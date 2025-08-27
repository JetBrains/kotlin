/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.phaser

import org.jetbrains.kotlin.backend.common.LoweringContext
import org.jetbrains.kotlin.config.LoggingContext
import org.jetbrains.kotlin.backend.common.ModuleLoweringPass
import org.jetbrains.kotlin.config.phaser.*
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment

// Phase composition.
private class CompositePhase<Context : LoggingContext, Input, Output>(
    val phases: List<CompilerPhase<Context, Any?, Any?>>
) : CompilerPhase<Context, Input, Output> {

    override fun invoke(phaseConfig: PhaseConfig, phaserState: PhaserState, context: Context, input: Input): Output {
        @Suppress("UNCHECKED_CAST")
        return phases.fold(input as Any?) { acc, phase ->
            phase.invoke(phaseConfig, phaserState, context, acc)
        } as Output
    }

    override fun getNamedSubphases(startDepth: Int): List<Pair<Int, NamedCompilerPhase<Context, *, *>>> =
        phases.flatMap { it.getNamedSubphases(startDepth) }
}

// used for building dynamic pipelines, where phases can repeat in a non trivial way
interface JvmPhaseCoordinator <Context : LoggingContext> {
    //TODO(emazhukin) mixing generics with inheritance.. there should be a nice way to implement the thing
    fun nextPhaseOrEnd(
        lastPhase: CompilerPhase<Context, Any?, Any?>?,
        mutableInput: Any?,
        phaseOutput: Any?
    ): CompilerPhase<Context, Any?, Any?>?
}

//TODO(emazhukin) move all dynamic pipeline stuff into FirRunner sourcefile?
class DynamicCompositePhase<Context : LoggingContext, Input, Output>(
    private val coordinator: JvmPhaseCoordinator<Context>
) : CompilerPhase<Context, Input, Output> {

    override fun invoke(phaseConfig: PhaseConfig, phaserState: PhaserState, context: Context, input: Input): Output {
        var nextPhase = coordinator.nextPhaseOrEnd(null, null, null)
        var previousOut: Any? = input
        var previousPhase: CompilerPhase<Context, Any?, Any?>? = null
        var recentOut: Any? = null
        while (nextPhase != null) {
            recentOut = nextPhase.invoke(phaseConfig, phaserState, context, previousOut)
            nextPhase = coordinator.nextPhaseOrEnd(nextPhase, previousOut, recentOut)
            if (nextPhase != previousPhase) {
                previousPhase = nextPhase
                previousOut = recentOut
            }
            // else we repeat the same phase with the previous phase's output as current phase input
            // it only makes sense if execution of the phase modifies a mutable part of the input
            // (so we could obtain a new result after doing the same thing)
        }
        @Suppress("UNCHECKED_CAST")
        return recentOut as Output
    }

    override fun getNamedSubphases(startDepth: Int): List<Pair<Int, NamedCompilerPhase<Context, *, *>>> {
        throw UnsupportedOperationException("in the dynamic pipeline list of phases may vary")
    }
}

@Suppress("UNCHECKED_CAST")
infix fun <Context : LoggingContext, Input, Mid, Output> CompilerPhase<Context, Input, Mid>.then(
    other: CompilerPhase<Context, Mid, Output>
): CompilerPhase<Context, Input, Output> {
    val unsafeThis = this as CompilerPhase<Context, Any?, Any?>
    val unsafeOther = other as CompilerPhase<Context, Any?, Any?>
    return CompositePhase(if (this is CompositePhase<Context, *, *>) phases + unsafeOther else listOf(unsafeThis, unsafeOther))
}

fun <Context : LoggingContext, Input, Output> createSimpleNamedCompilerPhase(
    name: String,
    preactions: Set<Action<Input, Context>> = emptySet(),
    postactions: Set<Action<Output, Context>> = emptySet(),
    prerequisite: Set<NamedCompilerPhase<*, *, *>> = emptySet(),
    outputIfNotEnabled: (PhaseConfig, PhaserState, Context, Input) -> Output,
    op: (Context, Input) -> Output
): NamedCompilerPhase<Context, Input, Output> = object : NamedCompilerPhase<Context, Input, Output>(
    name,
    preactions = preactions,
    postactions = postactions.map { f ->
        fun(actionState: ActionState, data: Pair<Input, Output>, context: Context) = f(actionState, data.second, context)
    }.toSet(),
    prerequisite = prerequisite,
) {
    override fun outputIfNotEnabled(phaseConfig: PhaseConfig, phaserState: PhaserState, context: Context, input: Input): Output =
        outputIfNotEnabled(phaseConfig, phaserState, context, input)

    override fun phaseBody(context: Context, input: Input): Output =
        op(context, input)
}

fun <Context : LoggingContext, Input> createSimpleNamedCompilerPhase(
    name: String,
    preactions: Set<Action<Input, Context>> = emptySet(),
    postactions: Set<Action<Input, Context>> = emptySet(),
    prerequisite: Set<NamedCompilerPhase<*, *, *>> = emptySet(),
    op: (Context, Input) -> Unit
): NamedCompilerPhase<Context, Input, Unit> = object : NamedCompilerPhase<Context, Input, Unit>(
    name,
    preactions = preactions,
    postactions = postactions.map { f ->
        fun(actionState: ActionState, data: Pair<Input, Unit>, context: Context) = f(actionState, data.first, context)
    }.toSet(),
    prerequisite = prerequisite,
) {
    override fun outputIfNotEnabled(phaseConfig: PhaseConfig, phaserState: PhaserState, context: Context, input: Input) {}

    override fun phaseBody(context: Context, input: Input): Unit =
        op(context, input)
}

fun <Context : LoweringContext> makeIrModulePhase(
    lowering: (Context) -> ModuleLoweringPass,
    name: String,
    prerequisite: Set<NamedCompilerPhase<Context, *, *>> = emptySet(),
    preconditions: Set<Action<IrModuleFragment, Context>> = emptySet(),
    postconditions: Set<Action<IrModuleFragment, Context>> = emptySet(),
): NamedCompilerPhase<Context, IrModuleFragment, IrModuleFragment> =
    createSimpleNamedCompilerPhase(
        name = name,
        preactions = DEFAULT_IR_ACTIONS + preconditions,
        postactions = DEFAULT_IR_ACTIONS + postconditions,
        prerequisite = prerequisite,
        outputIfNotEnabled = { _, _, _, irModule -> irModule },
        op = { context, irModule ->
            lowering(context).lower(irModule)
            irModule
        },
    )
