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
import org.jetbrains.kotlin.fir.resolve.diagnostics.collectors.AbstractDiagnosticCollector
import org.jetbrains.kotlin.fir.resolve.diagnostics.collectors.FirDiagnosticsCollector
import org.jetbrains.kotlin.fir.resolve.transformers.FirTotalResolveTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
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
abstract class AbstractFirDiagnosticsTest : AbstractFirBaseDiagnosticsTest() {
    companion object {
        val DUMP_CFG_DIRECTIVE = "DUMP_CFG"

        val TestFile.withDumpCfgDirective: Boolean
            get() = DUMP_CFG_DIRECTIVE in directives

        val File.cfgDumpFile: File
            get() = File(absolutePath.replace(".kt", ".dot"))
    }

    override fun runAnalysis(testDataFile: File, testFiles: List<TestFile>, firFilesPerSession: Map<FirSession, List<FirFile>>) {
        for ((_, firFiles) in firFilesPerSession) {
            doFirResolveTestBench(
                firFiles,
                FirTotalResolveTransformer().transformers,
                gc = false
            )
        }
        val allFirFiles = firFilesPerSession.values.flatten()
        checkDiagnostics(testDataFile, testFiles, allFirFiles)
        checkFir(testDataFile, allFirFiles)

        if (testFiles.any { it.withDumpCfgDirective }) {
            checkCfg(testDataFile, allFirFiles)
            checkCfgEdgeConsistency(allFirFiles)
        } else {
            checkCfgDumpNotExists(testDataFile)
        }
    }

    fun checkFir(testDataFile: File, firFiles: List<FirFile>) {
        val firFileDump = StringBuilder().apply { firFiles.forEach { it.accept(FirRenderer(this), null) } }.toString()
        val expectedPath = testDataFile.absolutePath.replace(".kt", ".txt")
        KotlinTestUtils.assertEqualsToFile(
            File(expectedPath),
            firFileDump
        )
    }

    protected fun checkDiagnostics(file: File, testFiles: List<TestFile>, firFiles: List<FirFile>) {
        val collector = createCollector()
        val actualText = StringBuilder()
        for (testFile in testFiles) {
            val firFile = firFiles.firstOrNull { it.psi == testFile.ktFile }
            if (firFile != null) {
                val coneDiagnostics = collector.collectDiagnostics(firFile)
                testFile.getActualText(coneDiagnostics, actualText)
            } else {
                actualText.append(testFile.expectedText)
            }
        }
        KotlinTestUtils.assertEqualsToFile(file, actualText.toString())
    }

    protected fun createCollector(): AbstractDiagnosticCollector {
        return FirDiagnosticsCollector.create()
    }

    private fun checkCfg(testDataFile: File, firFiles: List<FirFile>) {
        val builder = StringBuilder()

        firFiles.first().accept(FirControlFlowGraphRenderVisitor(builder), null)

        val dotCfgDump = builder.toString()
        KotlinTestUtils.assertEqualsToFile(testDataFile.cfgDumpFile, dotCfgDump)
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

    private fun checkCfgDumpNotExists(testDataFile: File) {
        val cfgDumpFile = testDataFile.cfgDumpFile
        if (cfgDumpFile.exists()) {
            val message = """
                Directive `!$DUMP_CFG_DIRECTIVE` is missing, but file with cfg dump is present.
                Please remove ${cfgDumpFile.path} or add `!$DUMP_CFG_DIRECTIVE` to test
            """.trimIndent()
            kotlin.test.fail(message)
        }

    }
}