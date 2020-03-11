/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dfa.cfg

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirRenderer
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.FirDoWhileLoop
import org.jetbrains.kotlin.fir.expressions.FirLoop
import org.jetbrains.kotlin.fir.expressions.FirWhileLoop
import org.jetbrains.kotlin.fir.expressions.impl.FirElseIfTrueCondition
import org.jetbrains.kotlin.fir.references.FirControlFlowGraphReference
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.resolve.dfa.FirControlFlowGraphReferenceImpl
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import org.jetbrains.kotlin.utils.DFS
import org.jetbrains.kotlin.utils.Printer
import java.util.*

class FirControlFlowGraphRenderVisitor(
    builder: StringBuilder,
) : FirVisitorVoid() {
    companion object {
        private const val EDGE = " -> "
        private const val RED = "red"
        private const val BLUE = "blue"

        private val EDGE_STYLE = EnumMap(
            mapOf(
                EdgeKind.Simple to "",
                EdgeKind.Dead to "[style=dotted]",
                EdgeKind.Cfg to "[color=green]",
                EdgeKind.Dfg to "[color=red]",
            )
        )
    }

    private val printer = Printer(builder)

    private var indexOffset = 0
    private var clusterCounter = 0

    override fun visitFile(file: FirFile) {
        printer
            .println("digraph ${file.name.replace(".", "_")} {")
            .pushIndent()
            .println("graph [nodesep=3]")
            .println("node [shape=box penwidth=2]")
            .println("edge [penwidth=2]")
            .println()
        visitElement(file)
        printer
            .popIndent()
            .println("}")
    }

    override fun visitElement(element: FirElement) {
        element.acceptChildren(this)
    }

    override fun visitControlFlowGraphReference(controlFlowGraphReference: FirControlFlowGraphReference) {
        val controlFlowGraph = (controlFlowGraphReference as? FirControlFlowGraphReferenceImpl)?.controlFlowGraph ?: return
        indexOffset = controlFlowGraph.dotRenderToStringBuilder(printer)
        printer.println()
    }

    private fun Printer.enterCluster(color: String) {
        println("subgraph cluster_${clusterCounter++} {")
        pushIndent()
        println("color=$color")
    }

    private fun Printer.exitCluster() {
        popIndent()
        println("}")
    }

    private fun ControlFlowGraph.dotRenderToStringBuilder(printer: Printer): Int {
        with(printer) {
            val graph = this@dotRenderToStringBuilder
            val sortedNodes = graph.sortNodes()
            val indices = sortedNodes.indicesMap().mapValues { (_, index) -> index + indexOffset }

            var color = RED
            sortedNodes.forEach {
                if (it is EnterNodeMarker) {
                    enterCluster(color)
                    color = BLUE
                }
                val attributes = mutableListOf<String>()
                attributes += "label=\"${it.render().replace("\"", "")}\""

                fun fillColor(color: String) {
                    attributes += "style=\"filled\""
                    attributes += "fillcolor=$color"
                }

                if (it == enterNode || it == exitNode) {
                    fillColor("red")
                }
                if (it.isDead) {
                    fillColor("gray")
                } else if (it is UnionFunctionCallArgumentsNode) {
                    fillColor("yellow")
                }
                println(indices.getValue(it), attributes.joinToString(separator = " ", prefix = " [", postfix = "];"))
                if (it is ExitNodeMarker) {
                    exitCluster()
                }
            }
            println()

            sortedNodes.forEachIndexed { i, node ->
                if (node.followingNodes.isEmpty()) return@forEachIndexed

                fun renderEdges(kind: EdgeKind) {
                    val edges = node.followingNodes.filter { node.outgoingEdges.getValue(it) == kind }
                    if (edges.isEmpty()) return
                    print(
                        i + indexOffset,
                        EDGE,
                        edges.joinToString(prefix = "{", postfix = "}", separator = " ") { indices.getValue(it).toString() }
                    )
                    EDGE_STYLE.getValue(kind).takeIf { it.isNotBlank() }?.let { printWithNoIndent(" $it") }
                    printlnWithNoIndent(";")
                }

                for (kind in EdgeKind.values()) {
                    renderEdges(kind)
                }
            }

            return indexOffset + sortedNodes.size
        }
    }
}

private object CfgRenderMode : FirRenderer.RenderMode(renderLambdaBodies = false, renderCallArguments = false)

private fun CFGNode<*>.render(): String =
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

                is QualifiedAccessNode -> "Access variable ${fir.calleeReference.render(CfgRenderMode)}"
                is OperatorCallNode -> "Operator ${fir.operation.operator}"
                is ComparisonExpressionNode -> "Comparison ${fir.operation.operator}"
                is TypeOperatorCallNode -> "Type operator: \"${fir.render(CfgRenderMode)}\""
                is JumpNode -> "Jump: ${fir.render()}"
                is StubNode -> "Stub"
                is CheckNotNullCallNode -> "Check not null: ${fir.render(CfgRenderMode)}"

                is ConstExpressionNode -> "Const: ${fir.render()}"
                is VariableDeclarationNode ->
                    "Variable declaration: ${buildString {
                        FirRenderer(
                            this,
                            CfgRenderMode
                        ).visitCallableDeclaration(fir)
                    }}"

                is VariableAssignmentNode -> "Assignmenet: ${fir.lValue.render(CfgRenderMode)}"
                is FunctionCallNode -> "Function call: ${fir.render(CfgRenderMode)}"
                is DelegatedConstructorCallNode -> "Delegated constructor call: ${fir.render(CfgRenderMode)}"
                is ThrowExceptionNode -> "Throw: ${fir.render(CfgRenderMode)}"

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

                is PostponedLambdaEnterNode -> "Postponed enter to lambda"
                is PostponedLambdaExitNode -> "Postponed exit from lambda"

                is AnonymousObjectExitNode -> "Exit anonymous object"

                is UnionFunctionCallArgumentsNode -> "Call arguments union"

                else -> TODO(this@render.toString())
            },
        )
    }

private fun FirFunction<*>.name(): String = when (this) {
    is FirSimpleFunction -> name.asString()
    is FirAnonymousFunction -> "anonymousFunction"
    is FirConstructor -> "<init>"
    is FirPropertyAccessor -> if (isGetter) "getter" else "setter"
    is FirErrorFunction -> "errorFunction"
    else -> TODO(toString())
}

private fun FirLoop.type(): String = when (this) {
    is FirWhileLoop -> "while"
    is FirDoWhileLoop -> "do-while"
    else -> throw IllegalArgumentException()
}

private fun ControlFlowGraph.sortNodes(): List<CFGNode<*>> {
    return DFS.topologicalOrder(nodes) {
        val result = if (it !is WhenBranchConditionExitNode || it.followingNodes.size < 2) {
            it.followingNodes
        } else {
            it.followingNodes.sortedBy { node -> if (node is BlockEnterNode) 1 else 0 }
        }
        result
    }
}

private fun List<CFGNode<*>>.indicesMap(): Map<CFGNode<*>, Int> = mapIndexed { i, node -> node to i }.toMap()
