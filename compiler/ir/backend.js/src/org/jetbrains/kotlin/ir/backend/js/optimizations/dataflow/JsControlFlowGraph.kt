/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.ir.backend.js.optimizations.dataflow

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrExpression

/**
 * Control-flow graph over existing IR statements (blocks hold references, not clones).
 *
 * Suitable substrate for sparse type/value dataflow, call-graph construction, and inlining
 * legality checks. Nested expression control flow that is not statement-level is left inside
 * the containing [IrStatement]; analyses may inspect those expressions directly.
 *
 * If [unsupportedConstruct] is non-null (e.g. [org.jetbrains.kotlin.ir.expressions.IrTry]),
 * the graph is incomplete for exception edges; analyses must treat the function conservatively.
 */
class JsControlFlowGraph(
    val function: IrFunction,
    val entry: JsBasicBlock,
    val blocks: List<JsBasicBlock>,
    val unsupportedConstruct: IrElement? = null,
)

/**
 * A straight-line sequence of IR statements ending in a single [terminator].
 */
class JsBasicBlock(
    var id: Int,
    val statements: MutableList<IrStatement> = mutableListOf(),
    var terminator: JsTerminator = JsTerminator.Unreachable,
) {
    val predecessors: MutableList<JsBasicBlock> = mutableListOf()
    val successors: MutableList<JsBasicBlock> = mutableListOf()

    override fun toString(): String = "BB$id"
}

/**
 * One arm of a [JsTerminator.MultiCond].
 *
 * @param condition branch predicate, or `null` for the else / fallthrough arm.
 * @param target block entered when this arm is taken.
 */
class MultiCondBranch(
    val condition: IrExpression?,
    val target: JsBasicBlock,
)

/**
 * Control transfer out of a [JsBasicBlock].
 */
sealed interface JsTerminator {
    /** Unconditional jump. */
    class Goto(val target: JsBasicBlock) : JsTerminator

    /** Two-way branch on [condition]. */
    class Cond(
        val condition: IrExpression,
        val thenTarget: JsBasicBlock,
        val elseTarget: JsBasicBlock,
    ) : JsTerminator

    /**
     * Multi-way branch (statement-level `when`).
     *
     * [branches] are in IR order. A [MultiCondBranch.condition] of `null` is the else /
     * fallthrough arm (appended by the builder when the `when` has no else). The first
     * matching arm is taken at runtime; analyses should treat arms as mutually exclusive
     * for narrowing and join the fallthrough with non-matching prefixes.
     */
    class MultiCond(val branches: List<MultiCondBranch>) : JsTerminator

    class Return(val value: IrExpression?) : JsTerminator

    class Throw(val value: IrExpression) : JsTerminator

    object Unreachable : JsTerminator
}

fun JsTerminator.successors(): List<JsBasicBlock> = when (this) {
    is JsTerminator.Goto -> listOf(target)
    is JsTerminator.Cond -> listOf(thenTarget, elseTarget)
    is JsTerminator.MultiCond -> branches.map { it.target }.distinct()
    is JsTerminator.Return, is JsTerminator.Throw, JsTerminator.Unreachable -> emptyList()
}
