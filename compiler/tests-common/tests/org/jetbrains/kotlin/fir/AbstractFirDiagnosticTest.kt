/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import com.intellij.psi.PsiElement
import junit.framework.TestCase
import org.jetbrains.kotlin.checkers.diagnostics.factories.DebugInfoDiagnosticFactory1
import org.jetbrains.kotlin.checkers.utils.TypeOfCall
import org.jetbrains.kotlin.diagnostics.rendering.Renderers
import org.jetbrains.kotlin.fir.analysis.collectors.AbstractDiagnosticCollector
import org.jetbrains.kotlin.fir.analysis.collectors.FirDiagnosticsCollector
import org.jetbrains.kotlin.fir.analysis.diagnostics.*
import org.jetbrains.kotlin.fir.declarations.FirCallableMemberDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirExpressionWithSmartcast
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.references.FirControlFlowGraphReference
import org.jetbrains.kotlin.fir.references.FirNamedReference
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.dfa.FirControlFlowGraphReferenceImpl
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.CFGNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ControlFlowGraph
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.EdgeKind
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.FirControlFlowGraphRenderVisitor
import org.jetbrains.kotlin.fir.resolve.transformers.createAllCompilerResolveProcessors
import org.jetbrains.kotlin.fir.symbols.AbstractFirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.coneTypeSafe
import org.jetbrains.kotlin.fir.visitors.FirDefaultVisitorVoid
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import org.jetbrains.kotlin.name.FqNameUnsafe
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.utils.addIfNotNull
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
        const val DUMP_CFG_DIRECTIVE = "DUMP_CFG"
        const val COMMON_COROUTINES_DIRECTIVE = "COMMON_COROUTINES_TEST"

        val TestFile.withDumpCfgDirective: Boolean
            get() = DUMP_CFG_DIRECTIVE in directives

        val TestFile.withCommonCoroutinesDirective: Boolean
            get() = COMMON_COROUTINES_DIRECTIVE in directives

        val File.cfgDumpFile: File
            get() = File(absolutePath.replace(".kt", ".dot"))
    }

    protected open val pluginPhasesEnabled: Boolean
        get() = false

    override fun runAnalysis(testDataFile: File, testFiles: List<TestFile>, firFilesPerSession: Map<FirSession, List<FirFile>>) {
        for ((session, firFiles) in firFilesPerSession) {
            doFirResolveTestBench(
                firFiles,
                createAllCompilerResolveProcessors(session, pluginPhasesEnabled = pluginPhasesEnabled),
                gc = false
            )
        }
        val allFirFiles = firFilesPerSession.values.flatten()
        checkDiagnostics(testDataFile, testFiles, allFirFiles)
        checkFir(testDataFile, allFirFiles)
        checkCfg(allFirFiles, testFiles, testDataFile)
    }

    protected fun checkCfg(
        allFirFiles: List<FirFile>,
        testFiles: List<TestFile>,
        testDataFile: File
    ) {
        checkCfgEdgeConsistency(allFirFiles)
        if (testFiles.any { it.withDumpCfgDirective }) {
            checkCfgDump(testDataFile, allFirFiles)
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

    protected open fun checkDiagnostics(file: File, testFiles: List<TestFile>, firFiles: List<FirFile>) {
        val diagnostics = collectDiagnostics(firFiles)
        val actualTextBuilder = StringBuilder()
        for (testFile in testFiles) {
            val firFile = firFiles.firstOrNull { it.psi == testFile.ktFile }
            if (firFile != null) {
                val debugInfoDiagnostics: List<FirDiagnostic<*>> =
                    collectDebugInfoDiagnostics(firFile, testFile.diagnosedRangesToDiagnosticNames)
                testFile.getActualText(
                    diagnostics.getValue(firFile) + debugInfoDiagnostics,
                    actualTextBuilder,
                )
            } else {
                actualTextBuilder.append(testFile.expectedText)
            }
        }
        var actualText = actualTextBuilder.toString()
        if (testFiles.any { it.withCommonCoroutinesDirective }) {
            actualText = actualText.replace(coroutinesPackage, "COROUTINES_PACKAGE")
        }
        KotlinTestUtils.assertEqualsToFile(file, actualText)
    }

    protected fun collectDebugInfoDiagnostics(
        firFile: FirFile,
        diagnosedRangesToDiagnosticNames: MutableMap<IntRange, MutableSet<String>>
    ): List<FirDiagnostic<*>> {
        val result = mutableListOf<FirDiagnostic<*>>()


        object : FirDefaultVisitorVoid() {
            override fun visitElement(element: FirElement) {
                if (element is FirExpression) {
                    result.addIfNotNull(
                        createExpressionTypeDiagnosticIfExpected(
                            element, diagnosedRangesToDiagnosticNames
                        )
                    )
                }

                element.acceptChildren(this)
            }

            override fun visitFunctionCall(functionCall: FirFunctionCall) {
                result.addIfNotNull(
                    createCallDiagnosticIfExpected(functionCall, functionCall.calleeReference, diagnosedRangesToDiagnosticNames)
                )

                super.visitFunctionCall(functionCall)
            }
        }.let(firFile::accept)

        return result
    }

    fun createExpressionTypeDiagnosticIfExpected(
        element: FirExpression,
        diagnosedRangesToDiagnosticNames: MutableMap<IntRange, MutableSet<String>>
    ): FirDiagnosticWithParameters1<FirSourceElement, String>? =
        DebugInfoDiagnosticFactory1.EXPRESSION_TYPE.createDebugInfoDiagnostic(element, diagnosedRangesToDiagnosticNames) {
            element.typeRef.renderAsString((element as? FirExpressionWithSmartcast)?.originalType)
        }

    private fun FirTypeRef.renderAsString(originalTypeRef: FirTypeRef?): String {
        val type = coneTypeSafe<ConeKotlinType>() ?: return "Type is unknown"
        val rendered = type.renderForDebugInfo()
        val originalTypeRendered = originalTypeRef?.coneTypeSafe<ConeKotlinType>()?.renderForDebugInfo() ?: return rendered

        return "$rendered & $originalTypeRendered"
    }

    private fun createCallDiagnosticIfExpected(
        element: FirElement,
        reference: FirNamedReference,
        diagnosedRangesToDiagnosticNames: MutableMap<IntRange, MutableSet<String>>
    ): FirDiagnosticWithParameters1<FirSourceElement, String>? =
        DebugInfoDiagnosticFactory1.CALL.createDebugInfoDiagnostic(element, diagnosedRangesToDiagnosticNames) {

            val resolvedSymbol = (reference as? FirResolvedNamedReference)?.resolvedSymbol
            val fqName = resolvedSymbol?.fqNameUnsafe()
            Renderers.renderCallInfo(fqName, getTypeOfCall(reference, resolvedSymbol))
        }

    private fun DebugInfoDiagnosticFactory1.createDebugInfoDiagnostic(
        element: FirElement,
        diagnosedRangesToDiagnosticNames: MutableMap<IntRange, MutableSet<String>>,
        argument: () -> String,
    ): FirDiagnosticWithParameters1<FirSourceElement, String>? {
        val sourceElement = element.source ?: return null
        val name = name ?: return null
        if (diagnosedRangesToDiagnosticNames[sourceElement.startOffset..sourceElement.endOffset]?.contains(name) != true) return null

        val argumentText = argument()
        return when (sourceElement) {
            is FirPsiSourceElement<*> -> FirPsiDiagnosticWithParameters1(
                sourceElement,
                argumentText,
                severity,
                FirDiagnosticFactory1(name, severity)
            )
            is FirLightSourceElement -> FirLightDiagnosticWithParameters1(
                sourceElement,
                argumentText,
                severity,
                FirDiagnosticFactory1<FirSourceElement, PsiElement, String>(name, severity)
            )
        }
    }

    private fun AbstractFirBasedSymbol<*>.fqNameUnsafe(): FqNameUnsafe? = when (this) {
        is FirClassLikeSymbol<*> -> classId.asSingleFqName().toUnsafe()
        is FirCallableSymbol<*> -> callableId.asFqNameForDebugInfo().toUnsafe()
        else -> null
    }

    private fun getTypeOfCall(
        reference: FirNamedReference,
        resolvedSymbol: AbstractFirBasedSymbol<*>?
    ): String {
        if (resolvedSymbol == null) return TypeOfCall.UNRESOLVED.nameToRender

        if ((resolvedSymbol as? FirFunctionSymbol)?.callableId?.callableName == OperatorNameConventions.INVOKE
            && reference.name != OperatorNameConventions.INVOKE
        ) {
            return TypeOfCall.VARIABLE_THROUGH_INVOKE.nameToRender
        }

        return when (val fir = resolvedSymbol.fir) {
            is FirProperty -> {
                TypeOfCall.PROPERTY_GETTER.nameToRender
            }
            is FirFunction<*> -> buildString {
                if (fir is FirCallableMemberDeclaration<*>) {
                    if (fir.status.isInline) append("inline ")
                    if (fir.status.isInfix) append("infix ")
                    if (fir.status.isOperator) append("operator ")
                    if (fir.receiverTypeRef != null) append("extension ")
                }
                append(TypeOfCall.FUNCTION.nameToRender)
            }
            else -> TypeOfCall.OTHER.nameToRender
        }
    }


    protected fun collectDiagnostics(firFiles: List<FirFile>): Map<FirFile, List<FirDiagnostic<*>>> {
        val collectors = mutableMapOf<FirSession, AbstractDiagnosticCollector>()
        val result = mutableMapOf<FirFile, List<FirDiagnostic<*>>>()
        for (firFile in firFiles) {
            val session = firFile.session
            val collector = collectors.computeIfAbsent(session) { createCollector(session) }
            result[firFile] = collector.collectDiagnostics(firFile).toList()
        }
        return result
    }

    private fun createCollector(session: FirSession): AbstractDiagnosticCollector {
        return FirDiagnosticsCollector.create(session)
    }

    private fun checkCfgDump(testDataFile: File, firFiles: List<FirFile>) {
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
            assertEquals(ControlFlowGraph.State.Completed, graph.state)
            checkConsistency(graph)
            checkOrder(graph)
        }

        private fun checkConsistency(graph: ControlFlowGraph) {
            for (node in graph.nodes) {
                for (to in node.followingNodes) {
                    checkEdge(node, to)
                }
                for (from in node.previousNodes) {
                    checkEdge(from, node)
                }
                if (node.followingNodes.isEmpty() && node.previousNodes.isEmpty()) {
                    throw AssertionError("Unconnected CFG node: $node")
                }
            }
        }

        private val cfgKinds = listOf(EdgeKind.DeadForward, EdgeKind.CfgForward, EdgeKind.DeadBackward, EdgeKind.CfgBackward)

        private fun checkEdge(from: CFGNode<*>, to: CFGNode<*>) {
            assertContainsElements(from.followingNodes, to)
            assertContainsElements(to.previousNodes, from)
            val fromKind = from.outgoingEdges.getValue(to).kind
            val toKind = to.incomingEdges.getValue(from).kind
            TestCase.assertEquals(fromKind, toKind)
            if (from.isDead && to.isDead) {
                assertContainsElements(cfgKinds, toKind)
            }
        }

        private fun checkOrder(graph: ControlFlowGraph) {
            val visited = mutableSetOf<CFGNode<*>>()
            for (node in graph.nodes) {
                for (previousNode in node.previousNodes) {
                    if (previousNode.owner != graph) continue
                    if (!node.incomingEdges.getValue(previousNode).kind.isBack) {
                        assertTrue(previousNode in visited)
                    }
                }
                visited += node
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
