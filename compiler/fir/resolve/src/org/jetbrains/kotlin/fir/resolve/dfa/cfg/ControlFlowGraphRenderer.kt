/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dfa.cfg

import org.jetbrains.kotlin.fir.FirRenderer
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.FirDoWhileLoop
import org.jetbrains.kotlin.fir.expressions.FirLoop
import org.jetbrains.kotlin.fir.expressions.FirWhileLoop
import org.jetbrains.kotlin.fir.expressions.impl.FirElseIfTrueCondition
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.utils.DFS

private const val INDENT = "  "
private const val DEAD = "[DEAD]"


fun List<CFGNode<*>>.indicesMap(): Map<CFGNode<*>, Int> = mapIndexed { i, node -> node to i }.toMap()

fun ControlFlowGraph.sortNodes(): List<CFGNode<*>> {
    return DFS.topologicalOrder(
        nodes
    ) {
        val result = if (it !is WhenBranchConditionExitNode || it.followingNodes.size < 2) {
            it.followingNodes
        } else {
            it.followingNodes.sortedBy { node -> if (node is BlockEnterNode) 1 else 0 }
        }
        result
    }
}

fun ControlFlowGraph.renderToStringBuilder(builder: StringBuilder) {
    val sortedNodes = sortNodes()

    val indices = sortedNodes.indicesMap()
    val notVisited = sortedNodes.toMutableSet()
    val maxLineNumberSize = sortedNodes.size.toString().length

    fun List<CFGNode<*>>.renderEdges(nodeIsDead: Boolean): String = map {
        indices.getValue(it) to it.isDead
    }.sortedBy { it.first }.joinToString(", ") { (index, isDead) ->
        index.toString() + if (isDead && !nodeIsDead) DEAD else ""
    }

    fun StringBuilder.renderNode(node: CFGNode<*>, index: Int) {
        append(index.toString().padStart(maxLineNumberSize))
        append(": ")
        append(INDENT.repeat(node.level))
        append(node.render())
        append(" -> ")
        append(node.followingNodes.renderEdges(node.isDead))
        if (node.previousNodes.isNotEmpty()) {
            append("  |  <- ")
            append(node.previousNodes.renderEdges(node.isDead))
        }
        appendln()
    }

    with(builder) {
        sortedNodes.forEachIndexed { i, node ->
            notVisited.remove(node)
            renderNode(node, i)
        }

        if (notVisited.isNotEmpty()) {
            appendln("Not visited nodes:")
            notVisited.forEach { node ->
                renderNode(node, indices.getValue(node))
            }
        }

        appendln()
    }
}

fun ControlFlowGraph.render(): String = buildString { renderToStringBuilder(this) }

fun CFGNode<*>.render(): String =
    buildString {
        append(
            when (this@render) {
                is FunctionEnterNode -> "Enter function \"${fir.name()}\""
                is FunctionExitNode -> "Exit function \"${fir.name()}\""

                is BlockEnterNode -> "Enter block"
                is BlockExitNode -> "Exit block"

                is WhenEnterNode -> "Enter when"
                is WhenBranchConditionEnterNode -> "Enter when branch condition ${if (fir.condition is FirElseIfTrueCondition) "\"else\"" else ""}"
                is WhenBranchConditionExitNode -> "Exit when branch condition"
                is WhenBranchResultEnterNode -> "Enter when branch result"
                is WhenBranchResultExitNode -> "Exit when branch result"
                is WhenSyntheticElseBranchNode -> "Synthetic else branch"
                is WhenExitNode -> "Exit when"

                is LoopEnterNode -> "Enter ${fir.type()} loop"
                is LoopBlockEnterNode -> "Enter loop block"
                is LoopBlockExitNode -> "Exit loop block"
                is LoopConditionEnterNode -> "Enter loop condition"
                is LoopConditionExitNode -> "Exit loop condition"
                is LoopExitNode -> "Exit ${fir.type()}loop"

                is QualifiedAccessNode -> "Access variable ${fir.calleeReference.render()}"
                is OperatorCallNode -> "Operator ${fir.operation.operator}"
                is TypeOperatorCallNode -> "Type operator: \"${fir.psi?.text?.toString() ?: fir.render()}\""
                is JumpNode -> "Jump: ${fir.render()}"
                is StubNode -> "Stub"

                is ConstExpressionNode -> "Const: ${fir.render()}"
                is VariableDeclarationNode ->
                    "Variable declaration: ${buildString { FirRenderer(this).visitCallableDeclaration(fir)} }"

                is VariableAssignmentNode -> "Assignmenet: ${fir.lValue.render()}"
                is FunctionCallNode -> "Function call: ${fir.render()}"
                is ThrowExceptionNode -> "Throw: ${fir.render()}"

                is TryExpressionEnterNode -> "Try expression enter"
                is TryMainBlockEnterNode -> "Try main block enter"
                is TryMainBlockExitNode -> "Try main block exit"
                is CatchClauseEnterNode -> "Catch enter"
                is CatchClauseExitNode -> "Catch exit"
                is FinallyBlockEnterNode -> "Enter finally"
                is FinallyBlockExitNode -> "Exit finally"
                is FinallyProxyEnterNode -> TODO()
                is FinallyProxyExitNode -> TODO()
                is TryExpressionExitNode -> "Try expression exit"

                is BinaryAndEnterNode -> "Enter &&"
                is BinaryAndExitLeftOperandNode -> "Exit left part of &&"
                is BinaryAndEnterRightOperandNode -> "Enter right part of &&"
                is BinaryAndExitNode -> "Exit &&"
                is BinaryOrEnterNode -> "Enter ||"
                is BinaryOrExitLeftOperandNode -> "Exit left part of ||"
                is BinaryOrEnterRightOperandNode -> "Enter right part of ||"
                is BinaryOrExitNode -> "Exit ||"

                is PropertyInitializerEnterNode -> "Enter property"
                is PropertyInitializerExitNode -> "Exit property"
                is InitBlockEnterNode -> "Enter init block"
                is InitBlockExitNode -> "Exit init block"
                is AnnotationEnterNode -> "Enter annotation"
                is AnnotationExitNode -> "Exit annotation"

                is EnterContractNode -> "Enter contract"
                is ExitContractNode -> "Exit contract"

                is EnterSafeCallNode -> "Enter safe call"
                is ExitSafeCallNode -> "Exit safe call"

                else -> TODO(this@render.toString())
            }
        )
    }

private fun FirFunction<*>.name(): String = when (this) {
    is FirSimpleFunction -> name.asString()
    is FirAnonymousFunction -> "anonymousFunction"
    is FirConstructor -> name.asString()
    is FirPropertyAccessor -> if (isGetter) "getter" else "setter"
    is FirErrorFunction -> "errorFunction"
    else -> TODO(toString())
}

private fun FirLoop.type(): String = when (this) {
    is FirWhileLoop -> "while"
    is FirDoWhileLoop -> "do-while"
    else -> throw IllegalArgumentException()
}