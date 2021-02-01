/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.phaser

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.lower
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment

// Phase composition.
private class CompositePhase<Context : CommonBackendContext, Input, Output>(
    val phases: List<CompilerPhase<Context, Any?, Any?>>
) : CompilerPhase<Context, Input, Output> {

    override fun invoke(phaseConfig: PhaseConfig, phaserState: PhaserState<Input>, context: Context, input: Input): Output {
        @Suppress("UNCHECKED_CAST") var currentState = phaserState as PhaserState<Any?>
        var result = phases.first().invoke(phaseConfig, currentState, context, input)
        for ((previous, next) in phases.zip(phases.drop(1))) {
            if (next !is SameTypeCompilerPhase<*, *>) {
                // Discard `stickyPostconditions`, they are useless since data type is changing.
                currentState = currentState.changeType()
            }
            currentState.stickyPostconditions.addAll(previous.stickyPostconditions)
            result = next.invoke(phaseConfig, currentState, context, result)
        }
        @Suppress("UNCHECKED_CAST")
        return result as Output
    }

    override fun getNamedSubphases(startDepth: Int): List<Pair<Int, NamedCompilerPhase<Context, *>>> =
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

fun <Context : CommonBackendContext, Element : IrElement> makeCustomPhase(
    op: (Context, Element) -> Unit,
    name: String,
    description: String,
    prerequisite: Set<NamedCompilerPhase<Context, *>> = emptySet(),
    preconditions: Set<Checker<Element>> = emptySet(),
    postconditions: Set<Checker<Element>> = emptySet(),
    stickyPostconditions: Set<Checker<Element>> = emptySet(),
    actions: Set<Action<Element, Context>> = setOf(defaultDumper, validationAction),
    nlevels: Int = 1
): NamedCompilerPhase<Context, Element> =
    NamedCompilerPhase(
        name, description, prerequisite, CustomPhaseAdapter(op), preconditions, postconditions, stickyPostconditions, actions, nlevels,
    )

private class CustomPhaseAdapter<Context : CommonBackendContext, Element>(
    private val op: (Context, Element) -> Unit
) : SameTypeCompilerPhase<Context, Element> {
    override fun invoke(phaseConfig: PhaseConfig, phaserState: PhaserState<Element>, context: Context, input: Element): Element {
        op(context, input)
        return input
    }
}

fun <Context : CommonBackendContext> namedUnitPhase(
    name: String,
    description: String,
    prerequisite: Set<NamedCompilerPhase<Context, *>> = emptySet(),
    nlevels: Int = 1,
    lower: CompilerPhase<Context, Unit, Unit>
): NamedCompilerPhase<Context, Unit> =
    NamedCompilerPhase(
        name, description, prerequisite, lower, nlevels = nlevels
    )

@Suppress("unused") // Used in kotlin-native
fun <Context : CommonBackendContext> namedOpUnitPhase(
    name: String,
    description: String,
    prerequisite: Set<NamedCompilerPhase<Context, *>>,
    op: Context.() -> Unit
): NamedCompilerPhase<Context, Unit> = namedUnitPhase(
    name, description, prerequisite,
    nlevels = 0,
    lower = object : SameTypeCompilerPhase<Context, Unit> {
        override fun invoke(phaseConfig: PhaseConfig, phaserState: PhaserState<Unit>, context: Context, input: Unit) {
            context.op()
        }
    }
)

fun <Context : CommonBackendContext> makeIrFilePhase(
    lowering: (Context) -> FileLoweringPass,
    name: String,
    description: String,
    prerequisite: Set<NamedCompilerPhase<Context, *>> = emptySet(),
    preconditions: Set<Checker<IrFile>> = emptySet(),
    postconditions: Set<Checker<IrFile>> = emptySet(),
    stickyPostconditions: Set<Checker<IrFile>> = emptySet(),
    actions: Set<Action<IrFile, Context>> = setOf(defaultDumper, validationAction)
): NamedCompilerPhase<Context, IrFile> =
    NamedCompilerPhase(
        name, description, prerequisite, FileLoweringPhaseAdapter(lowering), preconditions, postconditions, stickyPostconditions, actions,
        nlevels = 0,
    )

private class FileLoweringPhaseAdapter<Context : CommonBackendContext>(
    private val lowering: (Context) -> FileLoweringPass
) : SameTypeCompilerPhase<Context, IrFile> {
    override fun invoke(phaseConfig: PhaseConfig, phaserState: PhaserState<IrFile>, context: Context, input: IrFile): IrFile {
        lowering(context).lower(input)
        return input
    }
}

fun <Context : CommonBackendContext> makeIrModulePhase(
    lowering: (Context) -> FileLoweringPass,
    name: String,
    description: String,
    prerequisite: Set<NamedCompilerPhase<Context, *>> = emptySet(),
    preconditions: Set<Checker<IrModuleFragment>> = emptySet(),
    postconditions: Set<Checker<IrModuleFragment>> = emptySet(),
    stickyPostconditions: Set<Checker<IrModuleFragment>> = emptySet(),
    actions: Set<Action<IrModuleFragment, Context>> = setOf(defaultDumper, validationAction)
): NamedCompilerPhase<Context, IrModuleFragment> =
    NamedCompilerPhase(
        name, description, prerequisite, ModuleLoweringPhaseAdapter(lowering), preconditions, postconditions, stickyPostconditions, actions,
        nlevels = 0,
    )

private class ModuleLoweringPhaseAdapter<Context : CommonBackendContext>(
    private val lowering: (Context) -> FileLoweringPass
) : SameTypeCompilerPhase<Context, IrModuleFragment> {
    override fun invoke(
        phaseConfig: PhaseConfig, phaserState: PhaserState<IrModuleFragment>, context: Context, input: IrModuleFragment
    ): IrModuleFragment {
        lowering(context).lower(input)
        return input
    }
}

@Suppress("unused") // Used in kotlin-native
fun <Context : CommonBackendContext, Input> unitSink(): CompilerPhase<Context, Input, Unit> =
    object : CompilerPhase<Context, Input, Unit> {
        override fun invoke(phaseConfig: PhaseConfig, phaserState: PhaserState<Input>, context: Context, input: Input) {}
    }

// Intermediate phases to change the object of transformations
@Suppress("unused") // Used in kotlin-native
fun <Context : CommonBackendContext, OldData, NewData> takeFromContext(op: (Context) -> NewData): CompilerPhase<Context, OldData, NewData> =
    object : CompilerPhase<Context, OldData, NewData> {
        override fun invoke(phaseConfig: PhaseConfig, phaserState: PhaserState<OldData>, context: Context, input: OldData) = op(context)
    }

fun <Context : CommonBackendContext, OldData, NewData> transform(op: (OldData) -> NewData): CompilerPhase<Context, OldData, NewData> =
    object : CompilerPhase<Context, OldData, NewData> {
        override fun invoke(phaseConfig: PhaseConfig, phaserState: PhaserState<OldData>, context: Context, input: OldData) = op(input)
    }
