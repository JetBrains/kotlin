/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.phaser

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import kotlin.system.measureTimeMillis

class PhaserState<Data>(
    val alreadyDone: MutableSet<AnyNamedPhase> = mutableSetOf(),
    var depth: Int = 0,
    var phaseCount: Int = 0,
    val stickyPostconditions: MutableSet<Checker<Data>> = mutableSetOf()
)

// Copy state, forgetting the sticky postconditions (which will not be applicable to the new type)
fun <Input, Output> PhaserState<Input>.changeType() = PhaserState<Output>(alreadyDone, depth, phaseCount, mutableSetOf())

inline fun <R, D> PhaserState<D>.downlevel(nlevels: Int, block: () -> R): R {
    depth += nlevels
    val result = block()
    depth -= nlevels
    return result
}

interface CompilerPhase<in Context : CommonBackendContext, Input, Output> {
    fun invoke(phaseConfig: PhaseConfig, phaserState: PhaserState<Input>, context: Context, input: Input): Output

    fun getNamedSubphases(startDepth: Int = 0): List<Pair<Int, NamedCompilerPhase<Context, *>>> = emptyList()

    // In phase trees, `stickyPostconditions` is inherited along the right edge to be used in `then`.
    val stickyPostconditions: Set<Checker<Output>> get() = emptySet()
}

fun <Context : CommonBackendContext, Input, Output> CompilerPhase<Context, Input, Output>.invokeToplevel(
    phaseConfig: PhaseConfig,
    context: Context,
    input: Input
): Output = invoke(phaseConfig, PhaserState(), context, input)

interface SameTypeCompilerPhase<in Context : CommonBackendContext, Data> : CompilerPhase<Context, Data, Data>

// A failing checker should just throw an exception.
typealias Checker<Data> = (Data) -> Unit

typealias AnyNamedPhase = NamedCompilerPhase<*, *>

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

class NamedCompilerPhase<in Context : CommonBackendContext, Data>(
    val name: String,
    val description: String,
    val prerequisite: Set<NamedCompilerPhase<Context, *>> = emptySet(),
    private val lower: CompilerPhase<Context, Data, Data>,
    val preconditions: Set<Checker<Data>> = emptySet(),
    val postconditions: Set<Checker<Data>> = emptySet(),
    override val stickyPostconditions: Set<Checker<Data>> = emptySet(),
    private val actions: Set<Action<Data, Context>> = emptySet(),
    private val nlevels: Int = 0
) : SameTypeCompilerPhase<Context, Data> {
    override fun invoke(phaseConfig: PhaseConfig, phaserState: PhaserState<Data>, context: Context, input: Data): Data {
        if (this !in phaseConfig.enabled) {
            return input
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
        runAfter(phaseConfig, phaserState, context, output)

        phaserState.alreadyDone.add(this)
        phaserState.phaseCount++

        return output
    }

    private fun runBefore(phaseConfig: PhaseConfig, phaserState: PhaserState<Data>, context: Context, input: Data) {
        val state = ActionState(phaseConfig, this, phaserState.phaseCount, BeforeOrAfter.BEFORE)
        for (action in actions) action(state, input, context)

        if (phaseConfig.checkConditions) {
            for (pre in preconditions) pre(input)
        }
    }

    private fun runAfter(phaseConfig: PhaseConfig, phaserState: PhaserState<Data>, context: Context, output: Data) {
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

    private fun runAndProfile(phaseConfig: PhaseConfig, phaserState: PhaserState<Data>, context: Context, source: Data): Data {
        var result: Data? = null
        val msec = measureTimeMillis {
            result = phaserState.downlevel(nlevels) {
                lower.invoke(phaseConfig, phaserState, context, source)
            }
        }
        // TODO: use a proper logger
        println("${"\t".repeat(phaserState.depth)}$description: $msec msec")
        return result!!
    }

    override fun getNamedSubphases(startDepth: Int): List<Pair<Int, NamedCompilerPhase<Context, *>>> =
        listOf(startDepth to this) + lower.getNamedSubphases(startDepth + nlevels)

    override fun toString() = "Compiler Phase @$name"
}
