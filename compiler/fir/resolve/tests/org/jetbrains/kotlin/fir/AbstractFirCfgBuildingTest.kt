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

abstract class AbstractFirCfgBuildingTest : AbstractFirResolveTestCase() {
    override val configurationKind: ConfigurationKind
        get() = ConfigurationKind.ALL

    override fun doTest(path: String) {
        val firFiles = processInputFile(path)
        checkCfg(path, firFiles)
        checkFir(path, firFiles)
    }

    fun checkCfg(path: String, firFiles: List<FirFile>) {
        val simpleBuilder = StringBuilder()
        val dotBuilder = StringBuilder()

        firFiles.first().accept(FirControlFlowGraphRenderVisitor(simpleBuilder, dotBuilder), null)

        val dotCfgDump = dotBuilder.toString()
        val dotExpectedPath = path.replace(".kt", ".dot")
        KotlinTestUtils.assertEqualsToFile(File(dotExpectedPath), dotCfgDump)
    }

    private class FirControlFlowGraphRenderVisitor(
        private val simpleBuilder: StringBuilder,
        private val dotBuilder: StringBuilder
    ) : FirVisitorVoid() {
        private var indexOffset = 0

        override fun visitFile(file: FirFile) {
            dotBuilder.appendln("digraph ${file.name.replace(".", "_")} {")
            super.visitFile(file)
            dotBuilder.appendln("}")
        }

        override fun visitElement(element: FirElement) {
            element.acceptChildren(this)
        }

        override fun visitControlFlowGraphReference(controlFlowGraphReference: FirControlFlowGraphReference) {
            val controlFlowGraph = (controlFlowGraphReference as? FirControlFlowGraphReferenceImpl)?.controlFlowGraph ?: return
            controlFlowGraph.renderToStringBuilder(simpleBuilder)
            indexOffset = controlFlowGraph.dotRenderToStringBuilder(dotBuilder, indexOffset)
            dotBuilder.appendln()
        }
    }
}

private const val EDGE = " -> "
private const val INDENT = "  "

fun ControlFlowGraph.dotRenderToStringBuilder(builder: StringBuilder, indexOffset: Int): Int {
    with(builder) {
        val sortedNodes = sortNodes()
        val indices = sortedNodes.indicesMap().mapValues { (_, index) -> index + indexOffset }
        appendln("subgraph ${name.replace(" ", "_")} {")

        fun CFGNode<*>.splitEdges(): Pair<List<CFGNode<*>>, List<CFGNode<*>>> =
            if (isDead) emptyList<CFGNode<*>>() to followingNodes
            else followingNodes.filter { !it.isDead } to followingNodes.filter { it.isDead }

        sortedNodes.forEach {
            append(INDENT)
            append(indices.getValue(it))
            appendln(" [shape=box label=\"${it.render().replace("\"", "")}\"];")
        }
        appendln()

        sortedNodes.forEachIndexed { i, node ->
            if (node.followingNodes.isEmpty()) return@forEachIndexed
            val (aliveEdges, deadEdges) = node.splitEdges()

            fun renderEdges(edges: List<CFGNode<*>>, isDead: Boolean) {
                if (edges.isEmpty()) return
                append(INDENT)
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

        appendln("}")

        return indexOffset + sortedNodes.size
    }
}