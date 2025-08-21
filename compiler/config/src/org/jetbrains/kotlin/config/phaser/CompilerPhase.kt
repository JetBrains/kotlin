/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.config.phaser

import org.jetbrains.kotlin.config.LoggingContext
import kotlin.system.measureTimeMillis

/**
 * Represents global compilation context and stores information about phases that were executed.
 *
 * @property alreadyDone A set of already executed phases.
 * @property depth shows The index of the currently running phase.
 * @property phaseCount A unique ID that can show the order in which phases were executed.
 */
class PhaserState(
    val alreadyDone: MutableSet<AnyNamedPhase> = mutableSetOf(),
    var depth: Int = 0,
    var phaseCount: Int = 0,
) {
    fun copyOf() = PhaserState(alreadyDone.toMutableSet(), depth, phaseCount)
}

inline fun <R> PhaserState.downlevel(nlevels: Int, block: () -> R): R {
    depth += nlevels
    val result = block()
    depth -= nlevels
    return result
}

/**
 * Represents some compiler phase that can be executed.
 */
interface CompilerPhase<in Context : LoggingContext, Input, Output> {
    /**
     * Executes this compiler phase. It accepts some parameter of type [Input] and transforms it into [Output].
     *
     * @param phaseConfig Controls which parts of the compilation pipeline are enabled and how the compiler should validate their invariants.
     * @param phaserState The global context.
     * @param context The local context in which the compiler stores all the necessary information for the given phase.
     */
    fun invoke(phaseConfig: PhaseConfig, phaserState: PhaserState, context: Context, input: Input): Output

    fun getNamedSubphases(startDepth: Int = 0): List<Pair<Int, NamedCompilerPhase<Context, *, *>>> = emptyList()
}

fun <Context : LoggingContext, Input, Output> CompilerPhase<Context, Input, Output>.invokeToplevel(
    phaseConfig: PhaseConfig,
    context: Context,
    input: Input
): Output = invoke(phaseConfig, PhaserState(), context, input)

// A failing checker should just throw an exception.
typealias Checker<Data> = (Data) -> Unit

typealias AnyNamedPhase = NamedCompilerPhase<*, *, *>

enum class BeforeOrAfter { BEFORE, AFTER }

data class ActionState(
    val config: PhaseConfig,
    val phase: AnyNamedPhase,
    val phaseCount: Int,
    val beforeOrAfter: BeforeOrAfter
)

typealias Action<Data, Context> = (ActionState, Data, Context) -> Unit

infix operator fun <Data, Context> Action<Data, Context>.plus(other: Action<Data, Context>): Action<Data, Context> =
    { phaseState, data, context ->
        this(phaseState, data, context)
        other(phaseState, data, context)
    }

abstract class NamedCompilerPhase<in Context : LoggingContext, Input, Output>(
    val name: String,
    val prerequisite: Set<NamedCompilerPhase<*, *, *>> = emptySet(),
    val preconditions: Set<Checker<Input>> = emptySet(),
    val postconditions: Set<Checker<Output>> = emptySet(),
    private val preactions: Set<Action<Input, Context>> = emptySet(),
    private val postactions: Set<Action<Pair<Input, Output>, Context>> = emptySet(),
    protected val nlevels: Int = 0
) : CompilerPhase<Context, Input, Output> {
    override fun invoke(phaseConfig: PhaseConfig, phaserState: PhaserState, context: Context, input: Input): Output {
        if (!phaseConfig.isEnabled(this)) {
            return outputIfNotEnabled(phaseConfig, phaserState, context, input)
        }

        assert(phaserState.alreadyDone.containsAll(prerequisite)) {
            "Lowering $name: phases ${(prerequisite - phaserState.alreadyDone).map { it.name }} are required, but not satisfied"
        }

        context.inVerbosePhase = phaseConfig.isVerbose(this)

        runBefore(phaseConfig, phaserState, context, input)
        val output = if (phaseConfig.needProfiling) {
            runAndProfile(phaseConfig, phaserState, context, input)
        } else {
            phaserState.downlevel(nlevels) {
                phaseBody(context, input)
            }
        }
        runAfter(phaseConfig, phaserState, context, input, output)

        context.inVerbosePhase = false
        phaserState.alreadyDone.add(this)
        phaserState.phaseCount++

        return output
    }

    abstract fun phaseBody(context: Context, input: Input): Output

    abstract fun outputIfNotEnabled(phaseConfig: PhaseConfig, phaserState: PhaserState, context: Context, input: Input): Output

    private fun runBefore(phaseConfig: PhaseConfig, phaserState: PhaserState, context: Context, input: Input) {
        val state = ActionState(phaseConfig, this, phaserState.phaseCount, BeforeOrAfter.BEFORE)
        for (action in preactions) action(state, input, context)

        if (phaseConfig.checkConditions) {
            for (pre in preconditions) pre(input)
        }
    }

    private fun runAfter(phaseConfig: PhaseConfig, phaserState: PhaserState, context: Context, input: Input, output: Output) {
        val state = ActionState(phaseConfig, this, phaserState.phaseCount, BeforeOrAfter.AFTER)
        for (action in postactions) action(state, input to output, context)

        if (phaseConfig.checkConditions) {
            for (post in postconditions) post(output)
        }
    }

    private fun runAndProfile(phaseConfig: PhaseConfig, phaserState: PhaserState, context: Context, source: Input): Output {
        val result: Output
        val msec = measureTimeMillis {
            result = phaserState.downlevel(nlevels) {
                phaseBody(context, source)
            }
        }
        // TODO: use a proper logger
        println("${"\t".repeat(phaserState.depth)}$name: $msec msec")
        return result
    }

    override fun getNamedSubphases(startDepth: Int): List<Pair<Int, NamedCompilerPhase<Context, *, *>>> =
        listOf(startDepth to this)

    override fun toString() = "Compiler Phase @$name"
}
