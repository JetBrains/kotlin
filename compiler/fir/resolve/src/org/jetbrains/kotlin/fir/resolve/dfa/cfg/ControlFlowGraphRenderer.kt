/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("SpellCheckingInspection")

package org.jetbrains.kotlin.fir.resolve.dfa.cfg

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.references.FirControlFlowGraphReference
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.resolve.dfa.*
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import org.jetbrains.kotlin.utils.DFS
import org.jetbrains.kotlin.utils.Printer

private class ControlFlowGraphRenderer(
    builder: StringBuilder,
    private val options: ControlFlowGraphRenderOptions,
) {
    companion object {
        private const val EDGE = " -> "
        private const val RED = "red"
        private const val BLUE = "blue"

        private val DIGIT_REGEX = """\d""".toRegex()
    }

    private val printer = Printer(builder)

    private var nodeCounter = 0
    private var clusterCounter = 0

    fun renderCompleteGraph(graphName: String, printNodesAndEdges: () -> Unit) {
        var sanitizedName = graphName.replace(".", "_")
        if (sanitizedName.isNotEmpty() && DIGIT_REGEX.matches(sanitizedName.first().toString())) {
            sanitizedName = "_$sanitizedName"
        }
        printer
            .println("digraph $sanitizedName {")
            .pushIndent()
            .println("graph [nodesep=3]")
            .println("node [shape=box penwidth=2]")
            .println("edge [penwidth=2]")
            .println()
        printNodesAndEdges()
        printer
            .popIndent()
            .println("}")
    }

    fun renderPartialGraph(controlFlowGraph: ControlFlowGraph) {
        val nodes = DFS.topologicalOrder(listOf(controlFlowGraph.enterNode)) { it.followingNodes }
            .associateWithTo(linkedMapOf()) { nodeCounter++ }
        printer.renderNodes(nodes.filterKeys { it.level >= controlFlowGraph.enterNode.level })
        printer.renderEdges(nodes)
        printer.println()
    }

    private fun Printer.renderNodes(nodes: Map<CFGNode<*>, Int>) {
        var color = RED
        for ((node, index) in nodes) {
            if (node is EnterNodeMarker) {
                enterCluster(color)
                color = BLUE
            }
            val attributes = mutableListOf<String>()

            val nodeName = node.render()
            val nodeHeader = if (options.renderLevels) "$nodeName [${node.level}]" else nodeName
            val flowInfo = node.takeIf { options.renderFlow && it.flowInitialized }?.flow?.renderHtmlLike()
            if (!flowInfo.isNullOrEmpty()) {
                val label = buildString {
                    append("<TABLE BORDER=\"0\">")
                    append("<TR><TD>").append(nodeHeader.renderHtmlLike()).append("</TD></TR>")
                    append("<TR><TD ALIGN=\"LEFT\" BALIGN=\"LEFT\">").append(flowInfo).append("</TD></TR>")
                    append("</TABLE>")
                }
                attributes += "label=< $label >"
            } else {
                attributes += "label=\"${nodeHeader.replace("\"", "")}\""
            }

            when {
                node.isDead -> "gray"
                node == node.owner.enterNode || node == node.owner.exitNode -> "red"
                node.isUnion -> "yellow"
                else -> null
            }?.let {
                attributes += "style=\"filled\""
                attributes += "fillcolor=$it"
            }
            println(index, attributes.joinToString(separator = " ", prefix = " [", postfix = "];"))
            if (node is ExitNodeMarker) {
                exitCluster()
            }
        }
    }

    private val Edge.style: String?
        get() = listOfNotNull(
            when {
                !kind.usedInDfa && !kind.usedInDeadDfa -> "color=green"
                !kind.usedInCfa -> "color=red"
                else -> null
            },
            when {
                kind.isDead -> "style=dotted"
                kind.isBack -> "style=dashed"
                else -> null
            },
            label.label?.let { "label=\"$it\"" }
        ).ifEmpty { null }?.joinToString(prefix = "[", separator = " ", postfix = "]")

    private fun Printer.renderEdges(nodes: Map<CFGNode<*>, Int>) {
        for ((node, index) in nodes) {
            for ((style, group) in node.followingNodes.groupBy { node.edgeTo(it).style }.entries.sortedBy { it.key }) {
                val mappedGroup = group.map {
                    nodes.getValue(it)
                }.sorted()
                print(index, EDGE, mappedGroup.joinToString(prefix = "{", postfix = "}", separator = " "))
                style?.let { printWithNoIndent(" $it") }
                printlnWithNoIndent(";")
            }

            if (node is CFGNodeWithSubgraphs<*>) {
                val subNodes = node.subGraphs.mapNotNull { nodes[it.enterNode] }.sorted()
                if (subNodes.isNotEmpty()) {
                    print(index, EDGE, subNodes.joinToString(prefix = "{", postfix = "}", separator = " "))
                    printlnWithNoIndent(" [style=dashed];")
                }
            }
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

    private fun PersistentFlow.renderHtmlLike(): String {
        return buildString {
            for ((variable, variableName) in allVariablesForDebug.map { it to it.renderHtmlLike() }.sortedBy { it.second }) {
                append("<BR/>")
                append("<B>").append(variableName).append("</B>")
                if (variable is RealVariable) {
                    val aliased = unwrapVariable(variable)
                    if (aliased != variable) {
                        append(" = ").append(aliased.renderHtmlLike())
                    } else {
                        getTypeStatement(variable)?.let {
                            append(": ").append(it.exactType.renderHtmlLike())
                        }
                    }
                } else if (variable is SyntheticVariable) {
                    append(" = '").append(variable.fir.render().renderHtmlLike()).append("'")
                }
                getImplications(variable)?.forEach {
                    append("<BR/> ").append(it.condition.operation.renderHtmlLike())
                    append(" =&gt; ").append(it.effect.renderHtmlLike())
                }
                append("<BR/>")
            }
        }
    }

    private val firElementIndices = mutableMapOf<FirElement, Int>()

    private fun DataFlowVariable.renderHtmlLike(): String = when (this) {
        is RealVariable -> toString().renderHtmlLike()
        is SyntheticVariable -> "#${firElementIndices.getOrPut(fir) { firElementIndices.size }}"
    }

    private fun Statement.renderHtmlLike(): String = when (this) {
        is OperationStatement -> "${variable.renderHtmlLike()} ${operation.renderHtmlLike()}"
        is TypeStatement -> "${variable.renderHtmlLike()}: ${exactType.renderHtmlLike()}"
    }

    private fun Set<ConeKotlinType>.renderHtmlLike(): String =
        joinToString(separator = " & ").renderHtmlLike()

    /**
     * Sanitize string for rendering with HTML-like syntax.
     */
    private fun Any.renderHtmlLike(): String = toString()
        .replace("&", "&amp;")
        .replace(">", "&gt;")
        .replace("<", "&lt;")
}

data class ControlFlowGraphRenderOptions(val renderLevels: Boolean = false, val renderFlow: Boolean = false)

fun ControlFlowGraph.renderTo(builder: StringBuilder, options: ControlFlowGraphRenderOptions = ControlFlowGraphRenderOptions()) {
    ControlFlowGraphRenderer(builder, options).run {
        renderCompleteGraph(name) {
            renderPartialGraph(this@renderTo)
        }
    }
}

fun FirElement.renderControlFlowGraphTo(builder: StringBuilder, options: ControlFlowGraphRenderOptions = ControlFlowGraphRenderOptions()) {
    val graphName = (this@renderControlFlowGraphTo as? FirFile)?.name ?: ""
    ControlFlowGraphRenderer(builder, options).run {
        renderCompleteGraph(graphName) {
            accept(
                object : FirVisitorVoid() {
                    override fun visitElement(element: FirElement) {
                        element.acceptChildren(this)
                    }

                    override fun visitControlFlowGraphReference(controlFlowGraphReference: FirControlFlowGraphReference) {
                        val controlFlowGraph =
                            (controlFlowGraphReference as? FirControlFlowGraphReferenceImpl)?.controlFlowGraph ?: return
                        if (controlFlowGraph.isSubGraph) return
                        renderPartialGraph(controlFlowGraph)
                    }
                }
            )
        }
    }
}

@Suppress("unused") // Can be used from the debugger
fun ControlFlowGraph.render(options: ControlFlowGraphRenderOptions = ControlFlowGraphRenderOptions()): String =
    buildString {
        renderTo(this, options)
    }

@Suppress("unused") // Can be used from the debugger
fun FirElement.renderControlFlowGraph(options: ControlFlowGraphRenderOptions = ControlFlowGraphRenderOptions()): String =
    buildString {
        renderControlFlowGraphTo(this, options)
    }
