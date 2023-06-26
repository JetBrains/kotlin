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
    }

    private val printer = Printer(builder)

    private var nodeCounter = 0
    private var clusterCounter = 0

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
        printer
            .popIndent()
            .println("}")
    }

    private fun Printer.renderNodes(nodes: Map<CFGNode<*>, Int>) {
        var color = RED
        for ((node, index) in nodes) {
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
                val mappedGroup = group.map { nodes.getValue(it) }.sorted()
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

    override fun visitElement(element: FirElement) {
        element.acceptChildren(this)
    }

    override fun visitControlFlowGraphReference(controlFlowGraphReference: FirControlFlowGraphReference) {
        val controlFlowGraph = (controlFlowGraphReference as? FirControlFlowGraphReferenceImpl)?.controlFlowGraph ?: return
        if (controlFlowGraph.isSubGraph) return

        val nodes = DFS.topologicalOrder(listOf(controlFlowGraph.enterNode)) { it.followingNodes }
            .associateWithTo(linkedMapOf()) { nodeCounter++ }
        printer.renderNodes(nodes)
        printer.renderEdges(nodes)
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
}
