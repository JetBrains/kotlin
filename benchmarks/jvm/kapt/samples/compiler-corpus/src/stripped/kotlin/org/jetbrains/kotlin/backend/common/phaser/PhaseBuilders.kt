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

    override fun invoke(phaseConfig: PhaseConfig, phaserState: PhaserState, context: Context, input: Input): Output { TODO() }

    override fun getNamedSubphases(startDepth: Int): List<Pair<Int, NamedCompilerPhase<Context, *, *>>> = TODO()
}

@Suppress("UNCHECKED_CAST")
infix fun <Context : LoggingContext, Input, Mid, Output> CompilerPhase<Context, Input, Mid>.then(
    other: CompilerPhase<Context, Mid, Output>
): CompilerPhase<Context, Input, Output> { TODO() }

fun <Context : LoggingContext, Input, Mid, Output> CompilerPhase<Context, Input, Mid>.thenIf(
    condition: (Mid) -> Boolean,
    onTrue: CompilerPhase<Context, Mid, out Output>,
    onFalse: CompilerPhase<Context, Mid, out Output>
): CompilerPhase<Context, Input, Output> { TODO() }

fun <Context : LoggingContext, Input, Output> createSimpleNamedCompilerPhase(
    name: String,
    preactions: Set<Action<Input, Context>> = emptySet(),
    postactions: Set<Action<Output, Context>> = emptySet(),
    prerequisite: Set<NamedCompilerPhase<*, *, *>> = emptySet(),
    outputIfNotEnabled: (PhaseConfig, PhaserState, Context, Input) -> Output,
    op: (Context, Input) -> Output
): NamedCompilerPhase<Context, Input, Output> = TODO()

fun <Context : LoggingContext, Input> createSimpleNamedCompilerPhase(
    name: String,
    preactions: Set<Action<Input, Context>> = emptySet(),
    postactions: Set<Action<Input, Context>> = emptySet(),
    prerequisite: Set<NamedCompilerPhase<*, *, *>> = emptySet(),
    op: (Context, Input) -> Unit
): NamedCompilerPhase<Context, Input, Unit> = TODO()

fun <Context : LoweringContext> makeIrModulePhase(
    lowering: (Context) -> ModuleLoweringPass,
    name: String,
    prerequisite: Set<NamedCompilerPhase<Context, *, *>> = emptySet(),
    preconditions: Set<Action<IrModuleFragment, Context>> = emptySet(),
    postconditions: Set<Action<IrModuleFragment, Context>> = emptySet(),
): NamedCompilerPhase<Context, IrModuleFragment, IrModuleFragment> = TODO()