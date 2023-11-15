/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.phaser

import org.jetbrains.kotlin.backend.common.LoggingContext
import kotlin.system.measureTimeMillis

/**
 * Represents global compilation context and stores information about phases that were executed.
 *
 * @property alreadyDone A set of already executed phases.
 * @property depth shows The index of the currently running phase.
 * @property phaseCount A unique ID that can show the order in which phases were executed.
 * @property stickyPostconditions A set of conditions that must be checked after each phase.
 * When a condition is added into [stickyPostconditions], it will be executed each time some phase is executed, until we change [Data].
 */
class PhaserState<Data>(
    val alreadyDone: MutableSet<AnyNamedPhase> = mutableSetOf(),
    var depth: Int = 0,
    var phaseCount: Int = 0,
    val stickyPostconditions: MutableSet<Checker<Data>> = mutableSetOf()
) {
    fun copyOf() = PhaserState(alreadyDone.toMutableSet(), depth, phaseCount, stickyPostconditions)
}

// Copy state, forgetting the sticky postconditions (which will not be applicable to the new type)
fun <Input, Output> PhaserState<Input>.changePhaserStateType() = PhaserState<Output>(alreadyDone, depth, phaseCount, mutableSetOf())

inline fun <R, D> PhaserState<D>.downlevel(nlevels: Int, block: () -> R): R {
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
    fun invoke(phaseConfig: PhaseConfigurationService, phaserState: PhaserState<Input>, context: Context, input: Input): Output

    fun getNamedSubphases(startDepth: Int = 0): List<Pair<Int, AbstractNamedCompilerPhase<Context, *, *>>> = emptyList()

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

typealias AnyNamedPhase = AbstractNamedCompilerPhase<*, *, *>

enum class BeforeOrAfter { BEFORE, AFTER }

data class ActionState(
    val config: PhaseConfigurationService,
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

// TODO: A better name would be just `NamedCompilerPhase`, but it is already used (see below).
abstract class AbstractNamedCompilerPhase<in Context : LoggingContext, Input, Output>(
    val name: String,
    val description: String,
    val prerequisite: Set<AbstractNamedCompilerPhase<*, *, *>> = emptySet(),
    val preconditions: Set<Checker<Input>> = emptySet(),
    val postconditions: Set<Checker<Output>> = emptySet(),
    protected val nlevels: Int = 0
) : CompilerPhase<Context, Input, Output> {
    override fun invoke(phaseConfig: PhaseConfigurationService, phaserState: PhaserState<Input>, context: Context, input: Input): Output {
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
                phaseBody(phaseConfig, phaserState, context, input)
            }
        }
        runAfter(phaseConfig, changePhaserStateType(phaserState), context, input, output)

        phaserState.alreadyDone.add(this)
        phaserState.phaseCount++

        return output
    }

    abstract fun phaseBody(phaseConfig: PhaseConfigurationService, phaserState: PhaserState<Input>, context: Context, input: Input): Output

    abstract fun outputIfNotEnabled(phaseConfig: PhaseConfigurationService, phaserState: PhaserState<Input>, context: Context, input: Input): Output

    abstract fun changePhaserStateType(phaserState: PhaserState<Input>): PhaserState<Output>

    abstract fun runBefore(phaseConfig: PhaseConfigurationService, phaserState: PhaserState<Input>, context: Context, input: Input)

    abstract fun runAfter(phaseConfig: PhaseConfigurationService, phaserState: PhaserState<Output>, context: Context, input: Input, output: Output)

    private fun runAndProfile(phaseConfig: PhaseConfigurationService, phaserState: PhaserState<Input>, context: Context, source: Input): Output {
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

// TODO: This class should be named `SameTypeNamedCompilerPhase`,
//  but it would be a breaking change (e.g. there are usages in IntelliJ repo),
//  so we introduce a typealias instead as a temporary solution.
class NamedCompilerPhase<in Context : LoggingContext, Data>(
    name: String,
    description: String,
    prerequisite: Set<AbstractNamedCompilerPhase<*, *, *>> = emptySet(),
    private val lower: CompilerPhase<Context, Data, Data>,
    preconditions: Set<Checker<Data>> = emptySet(),
    postconditions: Set<Checker<Data>> = emptySet(),
    override val stickyPostconditions: Set<Checker<Data>> = emptySet(),
    private val actions: Set<Action<Data, Context>> = emptySet(),
    nlevels: Int = 0
) : AbstractNamedCompilerPhase<Context, Data, Data>(
    name, description, prerequisite, preconditions, postconditions, nlevels
) {
    override fun phaseBody(phaseConfig: PhaseConfigurationService, phaserState: PhaserState<Data>, context: Context, input: Data): Data =
        lower.invoke(phaseConfig, phaserState, context, input)

    override fun outputIfNotEnabled(phaseConfig: PhaseConfigurationService, phaserState: PhaserState<Data>, context: Context, input: Data): Data =
        input

    override fun changePhaserStateType(phaserState: PhaserState<Data>): PhaserState<Data> =
        phaserState

    override fun runBefore(phaseConfig: PhaseConfigurationService, phaserState: PhaserState<Data>, context: Context, input: Data) {
        val state = ActionState(phaseConfig, this, phaserState.phaseCount, BeforeOrAfter.BEFORE)
        for (action in actions) action(state, input, context)

        if (phaseConfig.checkConditions) {
            for (pre in preconditions) pre(input)
        }
    }

    override fun runAfter(phaseConfig: PhaseConfigurationService, phaserState: PhaserState<Data>, context: Context, input: Data, output: Data) {
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

    override fun getNamedSubphases(startDepth: Int): List<Pair<Int, AbstractNamedCompilerPhase<Context, *, *>>> =
        listOf(startDepth to this) + lower.getNamedSubphases(startDepth + nlevels)
}


typealias SameTypeNamedCompilerPhase<Context, Data> = NamedCompilerPhase<Context, Data>

/**
 * [AbstractNamedCompilerPhase] with different [Input] and [Output] types (unlike [SameTypeNamedCompilerPhase]).
 * Preffered when data should be explicitly passed between phases.
 * Actively used in a new dynamic Kotlin/Native driver.
 */
abstract class SimpleNamedCompilerPhase<in Context : LoggingContext, Input, Output>(
    name: String,
    description: String,
    prerequisite: Set<AbstractNamedCompilerPhase<*, *, *>> = emptySet(),
    preconditions: Set<Checker<Input>> = emptySet(),
    postconditions: Set<Checker<Output>> = emptySet(),
    private val preactions: Set<Action<Input, Context>> = emptySet(),
    private val postactions: Set<Action<Pair<Input, Output>, Context>> = emptySet(),
    nlevels: Int = 0,
) : AbstractNamedCompilerPhase<Context, Input, Output>(
    name,
    description,
    prerequisite,
    preconditions,
    postconditions,
    nlevels,
) {
    final override fun phaseBody(phaseConfig: PhaseConfigurationService, phaserState: PhaserState<Input>, context: Context, input: Input): Output =
        phaseBody(context, input)

    abstract fun phaseBody(context: Context, input: Input): Output

    override fun changePhaserStateType(phaserState: PhaserState<Input>): PhaserState<Output> =
        phaserState.changePhaserStateType()

    override fun runBefore(phaseConfig: PhaseConfigurationService, phaserState: PhaserState<Input>, context: Context, input: Input) {
        val state = ActionState(phaseConfig, this, phaserState.phaseCount, BeforeOrAfter.BEFORE)
        for (action in preactions) action(state, input, context)

        if (phaseConfig.checkConditions) {
            for (pre in preconditions) pre(input)
        }
    }

    override fun runAfter(phaseConfig: PhaseConfigurationService, phaserState: PhaserState<Output>, context: Context, input: Input, output: Output) {
        val state = ActionState(phaseConfig, this, phaserState.phaseCount, BeforeOrAfter.AFTER)
        for (action in postactions) action(state, input to output, context)

        if (phaseConfig.checkConditions) {
            for (post in postconditions) post(output)
            for (post in stickyPostconditions) post(output)
            if (phaseConfig.checkStickyConditions) {
                for (post in phaserState.stickyPostconditions) post(output)
            }
        }
    }

    override fun getNamedSubphases(startDepth: Int): List<Pair<Int, AbstractNamedCompilerPhase<Context, *, *>>> =
        listOf(startDepth to this)
}