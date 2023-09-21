/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.phaser

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.LoggingContext
import org.jetbrains.kotlin.backend.common.lower
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment

// Phase composition.
private class CompositePhase<Context : CommonBackendContext, Input, Output>(
    val phases: List<CompilerPhase<Context, Any?, Any?>>
) : CompilerPhase<Context, Input, Output> {

    override fun invoke(phaseConfig: PhaseConfigurationService, phaserState: PhaserState<Input>, context: Context, input: Input): Output {
        @Suppress("UNCHECKED_CAST") var currentState = phaserState as PhaserState<Any?>
        var result = phases.first().invoke(phaseConfig, currentState, context, input)
        for ((previous, next) in phases.zip(phases.drop(1))) {
            if (next !is SameTypeCompilerPhase<*, *>) {
                // Discard `stickyPostconditions`, they are useless since data type is changing.
                currentState = currentState.changePhaserStateType()
            }
            currentState.stickyPostconditions.addAll(previous.stickyPostconditions)
            result = next.invoke(phaseConfig, currentState, context, result)
        }
        @Suppress("UNCHECKED_CAST")
        return result as Output
    }

    override fun getNamedSubphases(startDepth: Int): List<Pair<Int, AbstractNamedCompilerPhase<Context, *, *>>> =
        phases.flatMap { it.getNamedSubphases(startDepth) }

    override val stickyPostconditions get() = phases.last().stickyPostconditions
}

@Suppress("UNCHECKED_CAST")
infix fun <Context : CommonBackendContext, Input, Mid, Output> CompilerPhase<Context, Input, Mid>.then(
    other: CompilerPhase<Context, Mid, Output>
): CompilerPhase<Context, Input, Output> {
    val unsafeThis = this as CompilerPhase<Context, Any?, Any?>
    val unsafeOther = other as CompilerPhase<Context, Any?, Any?>
    return CompositePhase(if (this is CompositePhase<Context, *, *>) phases + unsafeOther else listOf(unsafeThis, unsafeOther))
}

fun <Context : LoggingContext, Input, Output> createSimpleNamedCompilerPhase(
    name: String,
    description: String,
    preactions: Set<Action<Input, Context>> = emptySet(),
    postactions: Set<Action<Output, Context>> = emptySet(),
    prerequisite: Set<AbstractNamedCompilerPhase<*, *, *>> = emptySet(),
    outputIfNotEnabled: (PhaseConfigurationService, PhaserState<Input>, Context, Input) -> Output,
    op: (Context, Input) -> Output
): SimpleNamedCompilerPhase<Context, Input, Output> = object : SimpleNamedCompilerPhase<Context, Input, Output>(
    name,
    description,
    preactions = preactions,
    postactions = postactions.map { f ->
        fun(actionState: ActionState, data: Pair<Input, Output>, context: Context) = f(actionState, data.second, context)
    }.toSet(),
    prerequisite = prerequisite,
) {
    override fun outputIfNotEnabled(phaseConfig: PhaseConfigurationService, phaserState: PhaserState<Input>, context: Context, input: Input): Output =
        outputIfNotEnabled(phaseConfig, phaserState, context, input)

    override fun phaseBody(context: Context, input: Input): Output =
        op(context, input)
}

fun <Context : LoggingContext, Input> createSimpleNamedCompilerPhase(
    name: String,
    description: String,
    preactions: Set<Action<Input, Context>> = emptySet(),
    postactions: Set<Action<Input, Context>> = emptySet(),
    prerequisite: Set<AbstractNamedCompilerPhase<*, *, *>> = emptySet(),
    op: (Context, Input) -> Unit
): SimpleNamedCompilerPhase<Context, Input, Unit> = object : SimpleNamedCompilerPhase<Context, Input, Unit>(
    name,
    description,
    preactions = preactions,
    postactions = postactions.map { f ->
        fun(actionState: ActionState, data: Pair<Input, Unit>, context: Context) = f(actionState, data.first, context)
    }.toSet(),
    prerequisite = prerequisite,
) {
    override fun outputIfNotEnabled(phaseConfig: PhaseConfigurationService, phaserState: PhaserState<Input>, context: Context, input: Input) {}

    override fun phaseBody(context: Context, input: Input): Unit =
        op(context, input)
}

private val defaultConditions = setOf(defaultDumper, validationAction)

fun <Context : CommonBackendContext> makeCustomPhase(
    op: (Context, IrModuleFragment) -> Unit,
    name: String,
    description: String,
    prerequisite: Set<AbstractNamedCompilerPhase<Context, *, *>> = emptySet(),
    preconditions: Set<Action<IrModuleFragment, Context>> = emptySet(),
    postconditions: Set<Action<IrModuleFragment, Context>> = emptySet(),
): SimpleNamedCompilerPhase<Context, IrModuleFragment, IrModuleFragment> =
    createSimpleNamedCompilerPhase(
        name = name,
        description = description,
        preactions = defaultConditions + preconditions,
        postactions = defaultConditions + postconditions,
        prerequisite = prerequisite,
        outputIfNotEnabled = { _, _, _, irModule -> irModule },
        op = { context, irModule ->
            op(context, irModule)
            irModule
        },
    )

fun <Context : CommonBackendContext> makeIrFilePhase(
    lowering: (Context) -> FileLoweringPass,
    name: String,
    description: String,
    prerequisite: Set<AbstractNamedCompilerPhase<Context, *, *>> = emptySet(),
    preconditions: Set<Action<IrFile, Context>> = emptySet(),
    postconditions: Set<Action<IrFile, Context>> = emptySet(),
): SimpleNamedCompilerPhase<Context, IrFile, IrFile> =
    createSimpleNamedCompilerPhase(
        name = name,
        description = description,
        preactions = defaultConditions + preconditions,
        postactions = defaultConditions + postconditions,
        prerequisite = prerequisite,
        outputIfNotEnabled = { _, _, _, irFile -> irFile },
        op = { context, irFile ->
            lowering(context).lower(irFile)
            irFile
        },
    )

fun <Context : CommonBackendContext> makeIrModulePhase(
    lowering: (Context) -> FileLoweringPass,
    name: String,
    description: String,
    prerequisite: Set<AbstractNamedCompilerPhase<Context, *, *>> = emptySet(),
    preconditions: Set<Action<IrModuleFragment, Context>> = emptySet(),
    postconditions: Set<Action<IrModuleFragment, Context>> = emptySet(),
): SimpleNamedCompilerPhase<Context, IrModuleFragment, IrModuleFragment> =
    createSimpleNamedCompilerPhase(
        name = name,
        description = description,
        preactions = defaultConditions + preconditions,
        postactions = defaultConditions + postconditions,
        prerequisite = prerequisite,
        outputIfNotEnabled = { _, _, _, irModule -> irModule },
        op = { context, irModule ->
            lowering(context).lower(irModule)
            irModule
        },
    )
