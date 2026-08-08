/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.ir.backend.js.optimizations.dataflow

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.util.isElseBranch
import org.jetbrains.kotlin.ir.util.isTrueConst

/**
 * Builds a [JsControlFlowGraph] for a lowered [IrFunction].
 *
 * Statement-level `when` / loops / return / throw become terminators. Expression-nested
 * control flow stays inside the enclosing statement for analysis to inspect.
 */
fun buildJsFunctionCfg(function: IrFunction): JsControlFlowGraph {
    val body = function.body ?: return emptyGraph(function)
    val builder = JsControlFlowGraphBuilder()
    val entry = builder.newBlock()
    var current: JsBasicBlock? = entry
    when (body) {
        is IrBlockBody -> {
            for (statement in body.statements) {
                current = builder.emitStatement(statement, current) ?: break
            }
            current?.let { builder.finishWithReturn(it, value = null) }
        }
        is IrExpressionBody -> {
            current = builder.emitStatement(body.expression, current)
            current?.let { builder.finishWithReturn(it, body.expression) }
        }
        else -> builder.finishWithReturn(entry, value = null)
    }
    return builder.finish(function, entry)
}

private fun emptyGraph(function: IrFunction): JsControlFlowGraph {
    val entry = JsBasicBlock(id = 0, terminator = JsTerminator.Return(null))
    return JsControlFlowGraph(function, entry, listOf(entry))
}

private class JsControlFlowGraphBuilder {
    private val blocks = mutableListOf<JsBasicBlock>()
    private val loopStack = ArrayDeque<LoopContext>()
    private var unsupportedConstruct: IrElement? = null

    fun newBlock(): JsBasicBlock = JsBasicBlock(id = blocks.size).also { blocks += it }

    /**
     * Sets [from]'s terminator and wires predecessor/successor edges immediately.
     */
    fun link(from: JsBasicBlock, terminator: JsTerminator) {
        if (from.terminator !is JsTerminator.Unreachable) {
            return
        }
        from.terminator = terminator
        for (succ in terminator.successors()) {
            if (succ !in from.successors) from.successors += succ
            if (from !in succ.predecessors) succ.predecessors += from
        }
    }

    fun emitStatement(statement: IrStatement, current: JsBasicBlock?): JsBasicBlock? {
        if (current == null) return null
        return when (statement) {
            is IrWhen -> emitWhen(statement, current)
            is IrWhileLoop -> emitWhile(statement, current)
            is IrDoWhileLoop -> emitDoWhile(statement, current)
            is IrBreak -> {
                val target = loopStack.firstOrNull { it.loop == statement.loop }?.breakTarget
                if (target == null) {
                    // Non-enclosing loop: do not leave IrBreak as a statement.
                    if (unsupportedConstruct == null) unsupportedConstruct = statement
                    link(current, JsTerminator.Unreachable)
                    return null
                }
                link(current, JsTerminator.Goto(target))
                null
            }
            is IrContinue -> {
                val target = loopStack.firstOrNull { it.loop == statement.loop }?.continueTarget
                if (target == null) {
                    if (unsupportedConstruct == null) unsupportedConstruct = statement
                    link(current, JsTerminator.Unreachable)
                    return null
                }
                link(current, JsTerminator.Goto(target))
                null
            }
            is IrReturn -> {
                // Value lives only on the terminator, not also in statements.
                link(current, JsTerminator.Return(statement.value))
                null
            }
            is IrThrow -> {
                link(current, JsTerminator.Throw(statement.value))
                null
            }
            is IrBlock -> {
                var cur: JsBasicBlock? = current
                for (nested in statement.statements) {
                    cur = emitStatement(nested, cur) ?: return null
                }
                cur
            }
            is IrTry -> {
                if (unsupportedConstruct == null) unsupportedConstruct = statement
                current.statements += statement
                current
            }
            else -> {
                current.statements += statement
                current
            }
        }
    }

    private fun emitWhen(expression: IrWhen, current: JsBasicBlock): JsBasicBlock? {
        if (expression.branches.isEmpty()) {
            current.statements += expression
            return current
        }
        val only = expression.branches.singleOrNull()
        if (only != null && only.condition.isTrueConst()) {
            return emitStatement(only.result, current)
        }

        val merge = newBlock()
        val branchTargets = mutableListOf<MultiCondBranch>()
        for (branch in expression.branches) {
            val target = newBlock()
            branchTargets += MultiCondBranch(branch.condition, target)
            val after = emitStatement(branch.result, target)
            after?.let { link(it, JsTerminator.Goto(merge)) }
        }
        if (expression.branches.none { isElseBranch(it) }) {
            branchTargets += MultiCondBranch(condition = null, target = merge)
        }
        link(current, JsTerminator.MultiCond(branchTargets))
        return merge
    }

    private fun emitWhile(loop: IrWhileLoop, current: JsBasicBlock): JsBasicBlock {
        val header = newBlock()
        val body = newBlock()
        val exit = newBlock()
        link(current, JsTerminator.Goto(header))
        link(header, JsTerminator.Cond(loop.condition, thenTarget = body, elseTarget = exit))
        loopStack.addFirst(LoopContext(loop, breakTarget = exit, continueTarget = header))
        val loopBody = loop.body
        val afterBody = if (loopBody != null) emitStatement(loopBody, body) else body
        afterBody?.let { link(it, JsTerminator.Goto(header)) }
        loopStack.removeFirst()
        return exit
    }

    private fun emitDoWhile(loop: IrDoWhileLoop, current: JsBasicBlock): JsBasicBlock {
        val body = newBlock()
        val header = newBlock()
        val exit = newBlock()
        link(current, JsTerminator.Goto(body))
        loopStack.addFirst(LoopContext(loop, breakTarget = exit, continueTarget = header))
        val loopBody = loop.body
        val afterBody = if (loopBody != null) emitStatement(loopBody, body) else body
        afterBody?.let { link(it, JsTerminator.Goto(header)) }
        loopStack.removeFirst()
        link(header, JsTerminator.Cond(loop.condition, thenTarget = body, elseTarget = exit))
        return exit
    }

    fun finishWithReturn(block: JsBasicBlock, value: IrExpression?) {
        if (block.terminator is JsTerminator.Unreachable) {
            link(block, JsTerminator.Return(value))
        }
    }

    fun finish(function: IrFunction, entry: JsBasicBlock): JsControlFlowGraph {
        val reachable = pruneUnreachable(entry)
        return JsControlFlowGraph(function, entry, reachable, unsupportedConstruct)
    }

    private fun pruneUnreachable(entry: JsBasicBlock): List<JsBasicBlock> {
        val reachable = linkedSetOf<JsBasicBlock>()
        val queue = ArrayDeque<JsBasicBlock>()
        queue.add(entry)
        while (queue.isNotEmpty()) {
            val block = queue.removeFirst()
            if (!reachable.add(block)) continue
            for (succ in block.successors) queue.add(succ)
        }
        // Drop pred/succ edges that point outside the reachable set.
        for (block in reachable) {
            block.predecessors.retainAll(reachable)
            block.successors.retainAll(reachable)
        }
        val ordered = reachable.toList()
        for (indexedBlock in ordered.withIndex()) {
            indexedBlock.value.id = indexedBlock.index
        }
        return ordered
    }
}

private class LoopContext(
    val loop: IrLoop,
    val breakTarget: JsBasicBlock,
    val continueTarget: JsBasicBlock,
)
