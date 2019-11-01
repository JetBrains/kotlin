/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.references.FirControlFlowGraphReference
import org.jetbrains.kotlin.fir.resolve.dfa.FirControlFlowGraphReferenceImpl
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.*
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File
private const val EDGE = " -> "
private const val INDENT = "  "
private const val RED = "red"
private const val BLUE = "blue"

/*
 * For comfort viewing dumps of control flow graph you can setup external tool in IDEA that opens .dot files
 *
 * Example of config for `xdot` viewer:
 *
 * File -> Settings -> External tools -> Add
 *
 * Name: XDot
 * Program: xdot
 * Arguments: $FileNameWithoutExtension$.dot
 * Working directory: $FileDir$
 * Disable "Open console for tool output"
 *
 * After that you can run action `XDot` in editor with source of test (or with cfg dump)
 *   and it will opens xdot with dump for that test
 */
abstract class AbstractFirDiagnosticsWithCfgTest : AbstractFirDiagnosticsTest() {
    override fun getConfigurationKind(): ConfigurationKind {
        return ConfigurationKind.ALL
    }

    override fun runAnalysis(testDataFile: File, testFiles: List<TestFile>, firFiles: List<FirFile>) {
        super.runAnalysis(testDataFile, testFiles, firFiles)
        checkCfg(testDataFile, firFiles)
    }

    fun checkCfg(testDataFile: File, firFiles: List<FirFile>) {
        val simpleBuilder = StringBuilder()
        val dotBuilder = StringBuilder()

        firFiles.first().accept(FirControlFlowGraphRenderVisitor(simpleBuilder, dotBuilder), null)

        val dotCfgDump = dotBuilder.toString()
        val dotExpectedPath = testDataFile.absolutePath.replace(".kt", ".dot")
        KotlinTestUtils.assertEqualsToFile(File(dotExpectedPath), dotCfgDump)
    }

    private class FirControlFlowGraphRenderVisitor(
        private val simpleBuilder: StringBuilder,
        private val dotBuilder: StringBuilder
    ) : FirVisitorVoid() {
        private var indexOffset = 0
        private var clusterCounter = 0
        private var offset = 1

        override fun visitFile(file: FirFile) {
            dotBuilder.appendln("digraph ${file.name.replace(".", "_")} {")
                .appendln("${INDENT}graph [splines=ortho nodesep=3]")
                .appendln("${INDENT}node [shape=box penwidth=2]")
                .appendln("${INDENT}edge [penwidth=2]")
                .appendln()
            visitElement(file)
            dotBuilder.appendln("}")
        }

        override fun visitElement(element: FirElement) {
            element.acceptChildren(this)
        }

        override fun visitControlFlowGraphReference(controlFlowGraphReference: FirControlFlowGraphReference) {
            val controlFlowGraph = (controlFlowGraphReference as? FirControlFlowGraphReferenceImpl)?.controlFlowGraph ?: return
            controlFlowGraph.renderToStringBuilder(simpleBuilder)
            indexOffset = controlFlowGraph.dotRenderToStringBuilder(dotBuilder)
            dotBuilder.appendln()
        }

        private fun StringBuilder.enterCluster(color: String) {
            indent()
            appendln("subgraph cluster_${clusterCounter++} {")
            offset++
            indent()
            appendln("color=$color")
        }

        private fun StringBuilder.exitCluster() {
            offset--
            indent()
            appendln("}")
        }

        private fun StringBuilder.indent() {
            append(INDENT.repeat(offset))
        }

        fun ControlFlowGraph.dotRenderToStringBuilder(builder: StringBuilder): Int {
            with(builder) {
                val sortedNodes = sortNodes()
                val indices = sortedNodes.indicesMap().mapValues { (_, index) -> index + indexOffset }

                fun CFGNode<*>.splitEdges(): Pair<List<CFGNode<*>>, List<CFGNode<*>>> =
                    if (isDead) emptyList<CFGNode<*>>() to followingNodes
                    else followingNodes.filter { !it.isDead } to followingNodes.filter { it.isDead }

                var color = RED
                sortedNodes.forEach {
                    if (it is EnterNode) {
                        enterCluster(color)
                        color = BLUE
                    }
                    indent()
                    append(indices.getValue(it))
                    val attributes = mutableListOf<String>()
                    attributes += "label=\"${it.render().replace("\"", "")}\""
                    if (it == enterNode || it == exitNode) {
                        attributes += "style=\"filled\""
                        attributes += "fillcolor=red"
                    }
                    if (it.isDead) {
                        attributes += "style=\"filled\""
                        attributes += "fillcolor=gray"
                    }
                    appendln(attributes.joinToString(separator = " ", prefix = " [", postfix = "];"))
                    if (it is ExitNode) {
                        exitCluster()
                    }
                }
                appendln()

                sortedNodes.forEachIndexed { i, node ->
                    if (node.followingNodes.isEmpty()) return@forEachIndexed

                    val (aliveEdges, deadEdges) = node.splitEdges()

                    fun renderEdges(edges: List<CFGNode<*>>, isDead: Boolean) {
                        if (edges.isEmpty()) return
                        indent()
                        append(i + indexOffset)
                        append(EDGE)
                        append(edges.joinToString(prefix = "{", postfix = "}", separator = " ") { indices.getValue(it).toString() })
                        if (isDead) {
                            append(" [style=dotted]")
                        }
                        appendln(";")
                    }

                    renderEdges(aliveEdges, false)
                    renderEdges(deadEdges, true)
                }

                return indexOffset + sortedNodes.size
            }
        }
    }
}