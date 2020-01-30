/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import junit.framework.TestCase
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.references.FirControlFlowGraphReference
import org.jetbrains.kotlin.fir.resolve.dfa.FirControlFlowGraphReferenceImpl
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.CFGNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ControlFlowGraph
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.EdgeKind
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.FirControlFlowGraphRenderVisitor
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.testFramework.KtUsefulTestCase
import java.io.File


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

    override fun runAnalysis(testDataFile: File, testFiles: List<TestFile>, firFilesPerSession: Map<FirSession, List<FirFile>>) {
        super.runAnalysis(testDataFile, testFiles, firFilesPerSession)
        val allFirFiles = firFilesPerSession.values.flatten()
        checkCfg(testDataFile, allFirFiles)
        checkCfgEdgeConsistency(allFirFiles)
    }

    private fun checkCfg(testDataFile: File, firFiles: List<FirFile>) {
        val builder = StringBuilder()

        firFiles.first().accept(FirControlFlowGraphRenderVisitor(builder), null)

        val dotCfgDump = builder.toString()
        val dotExpectedPath = testDataFile.absolutePath.replace(".kt", ".dot")
        KotlinTestUtils.assertEqualsToFile(File(dotExpectedPath), dotCfgDump)
    }

    private fun checkCfgEdgeConsistency(firFiles: List<FirFile>) {
        firFiles.forEach { it.accept(CfgConsistencyChecker) }
    }

    private object CfgConsistencyChecker : FirVisitorVoid() {
        override fun visitElement(element: FirElement) {
            element.acceptChildren(this)
        }

        override fun visitControlFlowGraphReference(controlFlowGraphReference: FirControlFlowGraphReference) {
            val graph = (controlFlowGraphReference as? FirControlFlowGraphReferenceImpl)?.controlFlowGraph ?: return
            checkConsistency(graph)
        }

        private fun checkConsistency(graph: ControlFlowGraph) {
            for (node in graph.nodes) {
                for (to in node.followingNodes) {
                    checkEdge(node, to)
                }
                for (from in node.previousNodes) {
                    checkEdge(from, node)
                }
                TestCase.assertTrue(node.followingNodes.isNotEmpty() || node.previousNodes.isNotEmpty())
            }
        }

        private fun checkEdge(from: CFGNode<*>, to: CFGNode<*>) {
            KtUsefulTestCase.assertContainsElements(from.followingNodes, to)
            KtUsefulTestCase.assertContainsElements(to.previousNodes, from)
            val fromKind = from.outgoingEdges.getValue(to)
            val toKind = to.incomingEdges.getValue(from)
            TestCase.assertEquals(fromKind, toKind)
            if (from.isDead || to.isDead) {
                KtUsefulTestCase.assertContainsElements(listOf(EdgeKind.Dead, EdgeKind.Cfg), toKind)
            }
        }
    }
}