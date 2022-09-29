/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.phaser

import org.jetbrains.kotlin.backend.common.CommonBackendContext
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
    fun invoke(phaseConfig: PhaseConfigService, phaserState: PhaserState<Input>, context: Context, input: Input): Output

    fun getNamedSubphases(startDepth: Int = 0): List<Pair<Int, NamedCompilerPhase<Context, *, *>>> = emptyList()

    // In phase trees, `stickyPostconditions` is inherited along the right edge to be used in `then`.
    val stickyPostconditions: Set<Checker<Output>> get() = emptySet()
}

fun <Context : LoggingContext, Input, Output> CompilerPhase<Context, Input, Output>.invokeToplevel(
    phaseConfig: PhaseConfig,
    context: Context,
    input: Input
): Output = invoke(phaseConfig, PhaserState(), context, input)

interface SameTypeCompilerPhase<in Context : LoggingContext, Data> : CompilerPhase<Context, Data, Data>

// A failing checker should just throw an exception.
typealias Checker<Data> = (Data) -> Unit

typealias AnyNamedPhase = NamedCompilerPhase<*, *, *>

enum class BeforeOrAfter { BEFORE, AFTER }

data class ActionState(
    val config: PhaseConfigService,
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
    val name: String,
    val description: String,
    val prerequisite: Set<SameTypeNamedCompilerPhase<Context, *>> = emptySet(),
    val preconditions: Set<Checker<Input>> = emptySet(),
    val postconditions: Set<Checker<Output>> = emptySet(),
    protected val nlevels: Int = 0,
) : CompilerPhase<Context, Input, Output> {

    override fun invoke(phaseConfig: PhaseConfigService, phaserState: PhaserState<Input>, context: Context, input: Input): Output {
        if (!phaseConfig.isEnabled(this)) {
            return outputIfNotEnabled(context, input)
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
                phaseBody(phaseConfig, phaserState, context, input)
            }
        }

        runAfter(phaseConfig, changeType(phaserState), context, output)

        phaserState.alreadyDone.add(this)
        phaserState.phaseCount++

        return output
    }

    abstract fun outputIfNotEnabled(context: Context, input: Input): Output

    abstract fun phaseBody(phaseConfig: PhaseConfigService, phaserState: PhaserState<Input>, context: Context, input: Input): Output

    abstract fun changeType(phaserState: PhaserState<Input>): PhaserState<Output>

    abstract fun runBefore(phaseConfig: PhaseConfigService, phaserState: PhaserState<Input>, context: Context, input: Input)

    abstract fun runAfter(phaseConfig: PhaseConfigService, phaserState: PhaserState<Output>, context: Context, output: Output)

    private fun runAndProfile(phaseConfig: PhaseConfigService, phaserState: PhaserState<Input>, context: Context, source: Input): Output {
        var result: Output? = null
        val msec = measureTimeMillis {
            result = phaserState.downlevel(nlevels) {
                phaseBody(phaseConfig, phaserState, context, source)
            }
        }
        // TODO: use a proper logger
        println("${"\t".repeat(phaserState.depth)}$description: $msec msec")
        return result!!
    }

    override fun toString() = "Compiler Phase @$name"
}

class SameTypeNamedCompilerPhase<in Context : LoggingContext, Data>(
    name: String,
    description: String,
    prerequisite: Set<SameTypeNamedCompilerPhase<Context, *>> = emptySet(),
    private val lower: CompilerPhase<Context, Data, Data>,
    preconditions: Set<Checker<Data>> = emptySet(),
    postconditions: Set<Checker<Data>> = emptySet(),
    override val stickyPostconditions: Set<Checker<Data>> = emptySet(),
    private val actions: Set<Action<Data, Context>> = emptySet(),
    nlevels: Int = 0
) : NamedCompilerPhase<Context, Data, Data>(
    name,
    description,
    prerequisite,
    preconditions,
    postconditions,
    nlevels,
), SameTypeCompilerPhase<Context, Data> {

    override fun outputIfNotEnabled(context: Context, input: Data): Data =
        input

    override fun phaseBody(phaseConfig: PhaseConfigService, phaserState: PhaserState<Data>, context: Context, input: Data): Data =
        lower.invoke(phaseConfig, phaserState, context, input)

    override fun runBefore(phaseConfig: PhaseConfigService, phaserState: PhaserState<Data>, context: Context, input: Data) {
        val state = ActionState(phaseConfig, this, phaserState.phaseCount, BeforeOrAfter.BEFORE)
        for (action in actions) action(state, input, context)

        if (phaseConfig.checkConditions) {
            for (pre in preconditions) pre(input)
        }
    }

    override fun runAfter(phaseConfig: PhaseConfigService, phaserState: PhaserState<Data>, context: Context, output: Data) {
        val state = ActionState(phaseConfig, this, phaserState.phaseCount, BeforeOrAfter.AFTER)
        for (action in actions) action(state, output, context)

        if (phaseConfig.checkConditions) {
            for (post in postconditions) post(output)
            for (post in stickyPostconditions) post(output)
            if (phaseConfig.checkStickyConditions) {
                for (post in phaserState.stickyPostconditions) post(output)
            }
        }
    }

    override fun changeType(phaserState: PhaserState<Data>): PhaserState<Data> =
        phaserState

    override fun getNamedSubphases(startDepth: Int): List<Pair<Int, NamedCompilerPhase<Context, *, *>>> =
        listOf(startDepth to this) + lower.getNamedSubphases(startDepth + nlevels)
}

abstract class SimpleNamedCompilerPhase<in Context : LoggingContext, Input, Output>(
    name: String,
    description: String,
    prerequisite: Set<SameTypeNamedCompilerPhase<Context, *>> = emptySet(),
    preconditions: Set<Checker<Input>> = emptySet(),
    postconditions: Set<Checker<Output>> = emptySet(),
    private val preactions: Set<Action<Input, Context>> = emptySet(),
    private val postactions: Set<Action<Output, Context>> = emptySet(),
    nlevels: Int = 0,
) : NamedCompilerPhase<Context, Input, Output>(
    name,
    description,
    prerequisite,
    preconditions,
    postconditions,
    nlevels,
) {
    final override fun phaseBody(phaseConfig: PhaseConfigService, phaserState: PhaserState<Input>, context: Context, input: Input): Output =
        phaseBody(context, input)

    abstract fun phaseBody(context: Context, input: Input): Output

    override fun changeType(phaserState: PhaserState<Input>): PhaserState<Output> =
        phaserState.changeType()

    override fun runBefore(phaseConfig: PhaseConfigService, phaserState: PhaserState<Input>, context: Context, input: Input) {
        val state = ActionState(phaseConfig, this, phaserState.phaseCount, BeforeOrAfter.BEFORE)
        for (action in preactions) action(state, input, context)

        if (phaseConfig.checkConditions) {
            for (pre in preconditions) pre(input)
        }
    }

    override fun runAfter(phaseConfig: PhaseConfigService, phaserState: PhaserState<Output>, context: Context, output: Output) {
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

    override fun getNamedSubphases(startDepth: Int): List<Pair<Int, NamedCompilerPhase<Context, *, *>>> =
        listOf(startDepth to this)
}
