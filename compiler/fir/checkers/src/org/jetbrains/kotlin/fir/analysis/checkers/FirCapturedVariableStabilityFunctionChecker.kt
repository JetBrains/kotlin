/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.contracts.description.isInPlace
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactory1
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.analysis.cfa.util.*
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirFunctionChecker
import org.jetbrains.kotlin.fir.analysis.collectors.components.ControlFlowAnalysisDiagnosticComponent
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.toResolvedVariableSymbol
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.CFGNode
import org.jetbrains.kotlin.fir.resolve.dfa.controlFlowGraph
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.fir.types.ConeDynamicType
import org.jetbrains.kotlin.fir.visitors.FirDefaultVisitor
import kotlin.reflect.full.memberProperties

/**
 * Checks captured variables inside non-in-place lambdas and determines their stability
 * using [FindCapturedWrites, FindVisibleWrites].
 */
object FirCapturedVariableStabilityFunctionChecker : FirFunctionChecker(MppCheckerKind.Common) {
    @OptIn(SymbolInternals::class)
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirFunction) {
        if (context.containingElements.any { it is FirFunction && it != declaration }) return

        val graph = (declaration as? FirControlFlowGraphOwner)?.controlFlowGraphReference?.controlFlowGraph ?: return
        val collector = ControlFlowAnalysisDiagnosticComponent.LocalPropertyCollector().apply {
            declaration.acceptChildren(this, graph.subGraphs.toSet())
        }

        val capturedWrites = graph.traverseToFixedPoint(FindCapturedWrites(collector.properties))
        val visibleWrites = graph.traverseToFixedPoint(FindVisibleWrites(capturedWrites, collector.properties))

        val visitor = FirFunctionDeepVisitorWithData2()
        visitor.visitFunction(
            declaration,
            CapturedVariableCheckerData(
                context,
                reporter,
                visibleWrites = visibleWrites,
            )
        )
    }
}

data class CapturedVariableCheckerData(
    val context: CheckerContext,
    val reporter: DiagnosticReporter,
    val propertiesStack: MutableList<Set<FirPropertySymbol>> = mutableListOf(),
    val visibleWrites: Map<CFGNode<*>, PathAwareControlFlowInfo<PropertyAccessType, VariableWriteData>>,
    var currentWrite: FirQualifiedAccessExpression? = null
)

private class FirFunctionDeepVisitorWithData2 : FirDefaultVisitor<Unit, CapturedVariableCheckerData>() {
    override fun visitElement(element: FirElement, data: CapturedVariableCheckerData) {
        element.acceptChildren(this, data)
    }

    override fun visitAnonymousFunction(anonymousFunction: FirAnonymousFunction, data: CapturedVariableCheckerData) {
        val lambdaSymbol = anonymousFunction.symbol
        if (lambdaSymbol.inlineStatus == InlineStatus.Inline) return
        val invocationKind = anonymousFunction.invocationKind
        if (invocationKind.isInPlace) return

        val graph = (anonymousFunction as? FirControlFlowGraphOwner)?.controlFlowGraphReference?.controlFlowGraph ?: return

        val collector = ControlFlowAnalysisDiagnosticComponent.LocalPropertyCollector().apply {
            anonymousFunction.acceptChildren(this, graph.subGraphs.toSet())
        }

        data.propertiesStack.add(collector.properties)
        super.visitAnonymousFunction(anonymousFunction, data)
        data.propertiesStack.removeLast()
    }

    override fun visitQualifiedAccessExpression(
        qualifiedAccessExpression: FirQualifiedAccessExpression,
        data: CapturedVariableCheckerData,
    ) {
        qualifiedAccessExpression.checkExpressionCapturedVariable(data)
    }

    override fun visitVariableAssignment(
        variableAssignment: FirVariableAssignment,
        data: CapturedVariableCheckerData
    ) {
        val lValue = variableAssignment.lValue
        if (lValue is FirQualifiedAccessExpression) {
            data.currentWrite = lValue
        }
        super.visitVariableAssignment(variableAssignment, data)
        data.currentWrite = null
    }

    private fun isHasWrites(
        qualifiedAccessExpression: FirExpression,
        data: CapturedVariableCheckerData,
        propertySymbol: FirPropertySymbol
    ): Boolean {
        for ((node, _) in data.visibleWrites) {
            if (node.fir == qualifiedAccessExpression) {
                val accessNode = data.visibleWrites.keys.find { node ->
                    node.fir == qualifiedAccessExpression
                }

                val pathInfo = accessNode?.let { data.visibleWrites[it] }

                val hasCapturedWrite = pathInfo?.values?.any { controlFlowInfo ->
                    controlFlowInfo[PropertyAccessType.Captured]?.get(propertySymbol)?.isNotEmpty() == true
                } == true

                return hasCapturedWrite
            }
        }
        return false
    }

    private fun FirExpression.checkExpressionCapturedVariable(data: CapturedVariableCheckerData) {
        if (this is FirQualifiedAccessExpression) {
            val symbol = this.calleeReference.toResolvedVariableSymbol() ?: return
            val hasWrites = isHasWrites(this, data, symbol as? FirPropertySymbol ?: return)
            if (hasWrites || (data.currentWrite != null && data.currentWrite == this)) {
                checkCapturedVariable(symbol, data, this.source)
            }
            val receiver = this.explicitReceiver?.unwrapErrorExpression()?.unwrapArgument()
            receiver?.checkExpressionCapturedVariable(data)
        }
        if (this is FirCheckNotNullCall) {
            this.argument.checkExpressionCapturedVariable(data)
        }
        if (this is FirSafeCallExpression) {
            this.receiver.checkExpressionCapturedVariable(data)
        }
    }

    @OptIn(SymbolInternals::class)
    private fun checkCapturedVariable(variableSymbol: FirVariableSymbol<*>, data: CapturedVariableCheckerData, source: KtSourceElement?) {
        if (data.propertiesStack.isEmpty()) return
        if (variableSymbol in data.propertiesStack.last()) return
        if (variableSymbol.isVal) return
        if (variableSymbol.resolvedReturnType is ConeDynamicType) return
        if (!variableSymbol.isLocal) return
        val report = IEReporter(source, data.context, data.reporter, FirErrors.CV_DIAGNOSTIC)
        report(
            IEData(
                info = "Variable is captured from outer scope and is unstable in current scope",
                variableName = variableSymbol.name.toString(),
            )
        )
    }
}

class IEReporter(
    private val source: KtSourceElement?,
    private val context: CheckerContext,
    private val reporter: DiagnosticReporter,
    private val error: KtDiagnosticFactory1<String>,
) {
    operator fun invoke(v: IEData) {
        val dataStr = buildList {
            addAll(serializeData(v))
        }.joinToString("; ")
        val str = "$borderTag $dataStr $borderTag"
        reporter.reportOn(source, error, str, context)
    }

    private val borderTag: String = "KLEKLE"

    private fun serializeData(v: IEData): List<String> = buildList {
        v::class.memberProperties.forEach { property ->
            add("${property.name}: ${property.getter.call(v)}")
        }
    }
}

data class IEData(
    val info: String? = null,
    val variableName: String? = null,
    val leftmostReceiverName: String? = null,
)