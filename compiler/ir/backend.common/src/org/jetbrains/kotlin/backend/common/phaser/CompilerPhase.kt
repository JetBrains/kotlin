/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.phaser

import org.jetbrains.kotlin.backend.common.LoggingContext
import kotlin.system.measureTimeMillis

class PhaserState<Data>(
    val alreadyDone: MutableSet<AnyNamedPhase> = mutableSetOf(),
    var depth: Int = 0,
    var phaseCount: Int = 0,
    val stickyPostconditions: MutableSet<Checker<Data>> = mutableSetOf()
) {
    fun copyOf() = PhaserState(alreadyDone.toMutableSet(), depth, phaseCount, stickyPostconditions)
}

// Copy state, forgetting the sticky postconditions (which will not be applicable to the new type)
fun <Input, Output> PhaserState<Input>.changeType() = PhaserState<Output>(alreadyDone, depth, phaseCount, mutableSetOf())

inline fun <R, D> PhaserState<D>.downlevel(nlevels: Int, block: () -> R): R {
    depth += nlevels
    val result = block()
    depth -= nlevels
    return result
}

interface CompilerPhase<in Context : LoggingContext, Input, Output> {
    fun invoke(phaseConfig: PhaseConfig, phaserState: PhaserState<Input>, context: Context, input: Input): Output

    fun getNamedSubphases(startDepth: Int = 0): List<Pair<Int, CompilerPhaseWithName<Context, *, *>>> = emptyList()

    // In phase trees, `stickyPostconditions` is inherited along the right edge to be used in `then`.
    val stickyPostconditions: Set<Checker<Output>> get() = emptySet()
}

interface CompilerPhaseWithName<in Context : LoggingContext, Input, Output> : CompilerPhase<Context, Input, Output> {
    val name: String

    val description: String
}



fun <Context : LoggingContext, Input, Output> CompilerPhase<Context, Input, Output>.invokeToplevel(
    phaseConfig: PhaseConfig,
    context: Context,
    input: Input
): Output = invoke(phaseConfig, PhaserState(), context, input)

interface SameTypeCompilerPhase<in Context : LoggingContext, Data> : CompilerPhase<Context, Data, Data>

// A failing checker should just throw an exception.
typealias Checker<Data> = (Data) -> Unit

typealias AnyNamedPhase = CompilerPhaseWithName<*, *, *>

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


sealed class NamedCompilerPhase<in Context : LoggingContext, Input, Output>(
    val prerequisite: Set<AnyNamedPhase>,
    protected val lower: CompilerPhase<Context, Input, Output>,
    val preconditions: Set<Checker<Input>>,
    val postconditions: Set<Checker<Output>>,
    private val preactions: Set<Action<Input, Context>>,
    private val postactions: Set<Action<Output, Context>>,
    protected val nlevels: Int = 0,
) : CompilerPhaseWithName<Context, Input, Output> {

    abstract fun outputIfNotEnabled(context: Context, input: Input): Output

    override fun invoke(phaseConfig: PhaseConfig, phaserState: PhaserState<Input>, context: Context, input: Input): Output {
        if (this !in phaseConfig.enabled) {
            return outputIfNotEnabled(context, input)
        }

        assert(phaserState.alreadyDone.containsAll(prerequisite)) {
            "Lowering $name: phases ${(prerequisite - phaserState.alreadyDone).map { it.name }} are required, but not satisfied"
        }

        context.inVerbosePhase = this in phaseConfig.verbose

        runBefore(phaseConfig, phaserState, context, input)
        val output = if (phaseConfig.needProfiling) {
            runAndProfile(phaseConfig, phaserState, context, input)
        } else {
            phaserState.downlevel(nlevels) {
                lower.invoke(phaseConfig, phaserState, context, input)
            }
        }
        runAfter(phaseConfig, phaserState.changeType(), context, output)

        phaserState.alreadyDone.add(this)
        phaserState.phaseCount++

        return output
    }

    protected fun runAndProfile(phaseConfig: PhaseConfig, phaserState: PhaserState<Input>, context: Context, source: Input): Output {
        var result: Output? = null
        val msec = measureTimeMillis {
            result = phaserState.downlevel(nlevels) {
                lower.invoke(phaseConfig, phaserState, context, source)
            }
        }

        // TODO: use a proper logger
        println("${"\t".repeat(phaserState.depth)}$description: $msec msec")
        return result!!
    }

    private fun runBefore(phaseConfig: PhaseConfig, phaserState: PhaserState<Input>, context: Context, input: Input) {
        val state = ActionState(phaseConfig, this, phaserState.phaseCount, BeforeOrAfter.BEFORE)
        for (action in preactions) action(state, input, context)

        if (phaseConfig.checkConditions) {
            for (pre in preconditions) pre(input)
        }
    }

    private fun runAfter(phaseConfig: PhaseConfig, phaserState: PhaserState<Output>, context: Context, output: Output) {
        val state = ActionState(phaseConfig, this, phaserState.phaseCount, BeforeOrAfter.AFTER)
        for (action in postactions) action(state, output, context)

        if (phaseConfig.checkConditions) {
            for (post in postconditions) post(output)
            for (post in stickyPostconditions) post(output)
            if (phaseConfig.checkStickyConditions) {
                for (post in phaserState.stickyPostconditions) post(output)
            }
        }
    }

    override fun getNamedSubphases(startDepth: Int): List<Pair<Int, CompilerPhaseWithName<Context, *, *>>> =
        listOf(startDepth to this) + lower.getNamedSubphases(startDepth + nlevels)

    override fun toString() = "Compiler Phase @$name"

    operator fun <T> invoke() {

    }
}

class SimpleNamedCompilerPhase<in Context : LoggingContext, Input, Output>(
    override val name: String,
    override val description: String,
    prerequisite: Set<AnyNamedPhase> = emptySet(),
    lower: CompilerPhase<Context, Input, Output>,
    preconditions: Set<Checker<Input>> = emptySet(),
    postconditions: Set<Checker<Output>> = emptySet(),
    override val stickyPostconditions: Set<Checker<Output>> = emptySet(),
    preactions: Set<Action<Input, Context>> = emptySet(),
    postactions: Set<Action<Output, Context>> = emptySet(),
    nlevels: Int = 0,
    private val _outputIfNotEnabled: (Context, Input) -> Output
) : NamedCompilerPhase<Context, Input, Output>(prerequisite, lower, preconditions, postconditions, preactions, postactions, nlevels) {
    override fun outputIfNotEnabled(context: Context, input: Input): Output {
        return _outputIfNotEnabled(context, input)
    }
}

class UnitNamedCompilerPhase<in Context : LoggingContext, Output>(
    override val name: String,
    override val description: String,
    prerequisite: Set<AnyNamedPhase> = emptySet(),
    lower: CompilerPhase<Context, Unit, Output>,
    preconditions: Set<Checker<Unit>> = emptySet(),
    postconditions: Set<Checker<Output>> = emptySet(),
    override val stickyPostconditions: Set<Checker<Output>> = emptySet(),
    preactions: Set<Action<Unit, Context>> = emptySet(),
    postactions: Set<Action<Output, Context>> = emptySet(),
    nlevels: Int = 0,
    private val _outputIfNotEnabled: (Context) -> Output
) : NamedCompilerPhase<Context, Unit, Output>(prerequisite, lower, preconditions, postconditions, preactions, postactions, nlevels) {
    override fun outputIfNotEnabled(context: Context, input: Unit): Output {
        return _outputIfNotEnabled(context)
    }
}

class SameTypeNamedCompilerPhase<in Context : LoggingContext, Data>(
    override val name: String,
    override val description: String,
    prerequisite: Set<AnyNamedPhase> = emptySet(),
    lower: CompilerPhase<Context, Data, Data>,
    preconditions: Set<Checker<Data>> = emptySet(),
    postconditions: Set<Checker<Data>> = emptySet(),
    override val stickyPostconditions: Set<Checker<Data>> = emptySet(),
    actions: Set<Action<Data, Context>> = emptySet(),
    nlevels: Int = 0
) : NamedCompilerPhase<Context, Data, Data>(
    prerequisite,
    lower,
    preconditions,
    postconditions,
    preactions = actions,
    postactions = actions,
    nlevels
), SameTypeCompilerPhase<Context, Data> {

    override fun outputIfNotEnabled(context: Context, input: Data): Data =
        input
}
