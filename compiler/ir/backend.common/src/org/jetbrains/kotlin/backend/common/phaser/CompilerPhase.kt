/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.phaser

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import kotlin.system.measureTimeMillis

class PhaserState {
    val alreadyDone = mutableSetOf<AnyNamedPhase>()
    var depth = 0
}

fun <R> PhaserState.downlevel(nlevels: Int = 1, block: () -> R): R {
    depth += nlevels
    val result = block()
    depth -= nlevels
    return result
}

interface CompilerPhase<in Context : CommonBackendContext, in Input, out Output> {
    fun invoke(phaseConfig: PhaseConfig, phaserState: PhaserState, context: Context, input: Input): Output

    fun getNamedSubphases(startDepth: Int = 0): List<Pair<Int, NamedCompilerPhase<*, *, *>>> = emptyList()
}

fun <Context: CommonBackendContext, Input, Output> CompilerPhase<Context,  Input, Output>.invokeToplevel(
    phaseConfig: PhaseConfig,
    context: Context,
    input: Input
): Output = invoke(phaseConfig, PhaserState(), context, input)

interface SameTypeCompilerPhase<in Context: CommonBackendContext, Data> : CompilerPhase<Context, Data, Data>

interface NamedCompilerPhase<in Context : CommonBackendContext, in Input, out Output> : CompilerPhase<Context, Input, Output> {
    val name: String
    val description: String
    val prerequisite: Set<AnyNamedPhase> get() = emptySet()
}

typealias AnyNamedPhase = NamedCompilerPhase<*, *, *>

enum class BeforeOrAfter { BEFORE, AFTER }

interface PhaseDumperVerifier<in Context : CommonBackendContext, Data> {
    fun dump(phase: AnyNamedPhase, context: Context, data: Data, beforeOrAfter: BeforeOrAfter)
    fun verify(context: Context, data: Data)
}

abstract class AbstractNamedPhaseWrapper<in Context : CommonBackendContext, Input, Output>(
    override val name: String,
    override val description: String,
    override val prerequisite: Set<AnyNamedPhase>,
    private val nlevels: Int = 0,
    private val lower: CompilerPhase<Context, Input, Output>
) : NamedCompilerPhase<Context, Input, Output> {
    abstract val inputDumperVerifier: PhaseDumperVerifier<Context, Input>
    abstract val outputDumperVerifier: PhaseDumperVerifier<Context, Output>

    override fun invoke(phaseConfig: PhaseConfig, phaserState: PhaserState, context: Context, input: Input): Output {
        if (this is SameTypeCompilerPhase<*, *> &&
            this !in phaseConfig.enabled
        ) {
            return input as Output
        }

        assert(phaserState.alreadyDone.containsAll(prerequisite))
        context.inVerbosePhase = this in phaseConfig.verbose

        runBefore(phaseConfig, context, input)
        val output = runBody(phaseConfig, phaserState, context, input)
        runAfter(phaseConfig, context, output)

        phaserState.alreadyDone.add(this)

        return output
    }

    private fun runBefore(phaseConfig: PhaseConfig, context: Context, input: Input) {
        checkAndRun(phaseConfig.toDumpStateBefore) { inputDumperVerifier.dump(this, context, input, BeforeOrAfter.BEFORE) }
        checkAndRun(phaseConfig.toValidateStateBefore) { inputDumperVerifier.verify(context, input) }
    }

    private fun runBody(phaseConfig: PhaseConfig, phaserState: PhaserState, context: Context, input: Input): Output {
        return if (phaseConfig.needProfiling) {
            runAndProfile(phaseConfig, phaserState, context, input)
        } else {
            phaserState.downlevel(nlevels) {
                lower.invoke(phaseConfig, phaserState, context, input)
            }
        }
    }

    private fun runAfter(phaseConfig: PhaseConfig, context: Context, output: Output) {
        checkAndRun(phaseConfig.toDumpStateAfter) { outputDumperVerifier.dump(this, context, output, BeforeOrAfter.AFTER) }
        checkAndRun(phaseConfig.toValidateStateAfter) { outputDumperVerifier.verify(context, output) }
    }

    private fun runAndProfile(phaseConfig: PhaseConfig, phaserState: PhaserState, context: Context, source: Input): Output {
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

    private fun checkAndRun(set: Set<AnyNamedPhase>, block: () -> Unit) {
        if (this in set) block()
    }

    override fun getNamedSubphases(startDepth: Int): List<Pair<Int, NamedCompilerPhase<*, *, *>>> =
        listOf(startDepth to this) + lower.getNamedSubphases(startDepth + nlevels)

    override fun toString() = "Compiler Phase @$name"
}

class SameTypeNamedPhaseWrapper<in Context : CommonBackendContext, Data>(
    name: String,
    description: String,
    prerequisite: Set<AnyNamedPhase>,
    nlevels: Int = 0,
    lower: CompilerPhase<Context, Data, Data>,
    val dumperVerifier: PhaseDumperVerifier<Context, Data>
) : AbstractNamedPhaseWrapper<Context, Data, Data>(name, description, prerequisite, nlevels, lower), SameTypeCompilerPhase<Context, Data> {
    override val inputDumperVerifier get() = dumperVerifier
    override val outputDumperVerifier get() = dumperVerifier
}
