/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("SpellCheckingInspection")

package org.jetbrains.kotlin.fir.resolve.dfa.cfg

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.references.FirControlFlowGraphReference
import org.jetbrains.kotlin.fir.resolve.dfa.FirControlFlowGraphReferenceImpl
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import org.jetbrains.kotlin.utils.DFS
import org.jetbrains.kotlin.utils.Printer
import java.util.*

class FirControlFlowGraphRenderVisitor(
    builder: StringBuilder,
    private val renderLevels: Boolean = false
) : FirVisitorVoid() {
    companion object {
        private const val EDGE = " -> "
        private const val RED = "red"
        private const val BLUE = "blue"

        private val DIGIT_REGEX = """\d""".toRegex()

        private val EDGE_STYLE = EnumMap(
            mapOf(
                EdgeKind.Forward to "",
                EdgeKind.DeadForward to "[style=dotted]",
                EdgeKind.CfgForward to "[color=green]",
                EdgeKind.DfgForward to "[color=red]",
                EdgeKind.CfgBackward to "[color=green style=dashed]",
                EdgeKind.DeadBackward to "[color=green style=dotted]"
            )
        )
    }

    private val printer = Printer(builder)

    private var nodeCounter = 0
    private var clusterCounter = 0
    private val indices = mutableMapOf<CFGNode<*>, Int>()

    private val topLevelGraphs = mutableSetOf<ControlFlowGraph>()
    private val allGraphs = mutableSetOf<ControlFlowGraph>()

    override fun visitFile(file: FirFile) {
        var name = file.name.replace(".", "_")
        if (DIGIT_REGEX.matches(name.first().toString())) {
            name = "_$name"
        }
        printer
            .println("digraph $name {")
            .pushIndent()
            .println("graph [nodesep=3]")
            .println("node [shape=box penwidth=2]")
            .println("edge [penwidth=2]")
            .println()
        visitElement(file)

        for (topLevelGraph in topLevelGraphs) {
            printer.renderNodes(topLevelGraph)
            printer.renderEdges(topLevelGraph)
            printer.println()
        }

        printer
            .popIndent()
            .println("}")
    }

    private fun ControlFlowGraph.collectNodes() {
        for (node in nodes) {
            indices[node] = nodeCounter++
        }
    }

    private fun Printer.renderNodes(graph: ControlFlowGraph) {
        var color = RED
        val sortedNodes = graph.sortedNodes()
        for (node in sortedNodes) {
            if (node is EnterNodeMarker) {
                enterCluster(color)
                color = BLUE
            }
            val attributes = mutableListOf<String>()
            val label = buildString {
                append(node.render().replace("\"", ""))
                if (renderLevels) {
                    append(" [${node.level}]")
                }
            }
            attributes += "label=\"$label\""

            fun fillColor(color: String) {
                attributes += "style=\"filled\""
                attributes += "fillcolor=$color"
            }

            if (node == node.owner.enterNode || node == node.owner.exitNode) {
                fillColor("red")
            }
            if (node.isDead) {
                fillColor("gray")
            } else if (node is UnionFunctionCallArgumentsNode) {
                fillColor("yellow")
            }
            println(indices.getValue(node), attributes.joinToString(separator = " ", prefix = " [", postfix = "];"))
            if (node is ExitNodeMarker) {
                exitCluster()
            }
        }
    }

    private fun Printer.renderEdges(graph: ControlFlowGraph) {
        for (node in graph.nodes) {
            if (node.followingNodes.isEmpty()) continue

            fun renderEdges(kind: EdgeKind) {
                val edges = node.followingNodes.filter { node.outgoingEdges.getValue(it).kind == kind }
                if (edges.isEmpty()) return

                fun renderEdgesWithoutLabel(edges: List<CFGNode<*>>) {
                    print(
                        indices.getValue(node),
                        EDGE,
                        edges.joinToString(prefix = "{", postfix = "}", separator = " ") { indices.getValue(it).toString() }
                    )
                    EDGE_STYLE.getValue(kind).takeIf { it.isNotBlank() }?.let { printWithNoIndent(" $it") }
                    printlnWithNoIndent(";")
                }

                if (edges.any { node.outgoingEdges[it]?.label?.label != null }) {
                    val edgeGroups = edges.groupBy { node.outgoingEdges[it]?.label?.label != null }
                    edgeGroups[false]?.let { renderEdgesWithoutLabel(it) }
                    for (edge in edgeGroups[true]!!) {
                        print(
                            indices.getValue(node),
                            EDGE,
                            "{", indices.getValue(edge), "}"
                        )
                        EDGE_STYLE.getValue(kind).takeIf { it.isNotBlank() }?.let { printWithNoIndent(" $it") }
                        print("[label=${node.outgoingEdges[edge]!!.label}]")
                        printlnWithNoIndent(";")
                    }
                } else {
                    renderEdgesWithoutLabel(edges)
                }
            }

            for (kind in EdgeKind.values()) {
                renderEdges(kind)
            }

            if (node is CFGNodeWithCfgOwner<*>) {
                val subNodes = node.subGraphs
                if (subNodes.isNotEmpty()) {
                    print(
                        indices.getValue(node),
                        EDGE,
                        subNodes.mapNotNull { indices[it.enterNode] }.joinToString(prefix = "{", postfix = "}", separator = " ")
                    )
                    printWithNoIndent(" [style=dashed]")
                    printlnWithNoIndent(";")
                }
            }
        }
        for (subGraph in graph.subGraphs) {
            renderEdges(subGraph)
        }
    }

    override fun visitElement(element: FirElement) {
        element.acceptChildren(this)
    }

    override fun visitControlFlowGraphReference(controlFlowGraphReference: FirControlFlowGraphReference) {
        val controlFlowGraph = (controlFlowGraphReference as? FirControlFlowGraphReferenceImpl)?.controlFlowGraph ?: return
        initializeNodes(controlFlowGraph)
    }

    private fun initializeNodes(graph: ControlFlowGraph) {
        if (graph in allGraphs) return
        graph.collectNodes()
        if (graph.owner == null) {
            topLevelGraphs += graph
        }
        allGraphs += graph
        for (subGraph in graph.subGraphs) {
            initializeNodes(subGraph)
        }
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
}

private fun ControlFlowGraph.sortedNodes(): List<CFGNode<*>> {
    val nodesToSort = nodes.filterTo(mutableListOf()) { it != enterNode }
    val graphs = mutableSetOf(this)
    forEachSubGraph {
        nodesToSort += it.nodes
        graphs += it
    }

    val topologicalOrder = DFS.topologicalOrder(nodesToSort) {
        val result = if (it !is WhenBranchConditionExitNode || it.followingNodes.size < 2) {
            it.followingNodes
        } else {
            it.followingNodes.sortedBy { node -> if (node is BlockEnterNode) 1 else 0 }
        }.filter { node -> node.owner in graphs }
        result
    }
    return listOf(enterNode) + topologicalOrder
}

private fun ControlFlowGraph.forEachSubGraph(block: (ControlFlowGraph) -> Unit) {
    for (subGraph in subGraphs) {
        block(subGraph)
        subGraph.forEachSubGraph(block)
    }
}
