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
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirFunctionChecker
import org.jetbrains.kotlin.fir.analysis.collectors.components.ControlFlowAnalysisDiagnosticComponent
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.fir.declarations.FirControlFlowGraphOwner
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.InlineStatus
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirVariableAssignment
import org.jetbrains.kotlin.fir.references.toResolvedVariableSymbol
import org.jetbrains.kotlin.fir.resolve.dfa.controlFlowGraph
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.fir.types.ConeDynamicType
import org.jetbrains.kotlin.fir.visitors.FirDefaultVisitor
import kotlin.reflect.full.memberProperties

/**
 * Checks captured variables inside non-in-place lambdas and determines their stability
 * using [FirLocalVariableAssignmentAnalyzer].
 */
object FirCapturedVariableStabilityFunctionChecker : FirFunctionChecker(MppCheckerKind.Common) {
    @OptIn(SymbolInternals::class)
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirFunction) {
        // process functions, inside which can be FirAnonymousFunction
        if (declaration is FirAnonymousFunction) return
        val visitor = FirFunctionDeepVisitorWithData()
        declaration.accept(visitor, CapturedVariableCheckerData(context, reporter))
    }
}

data class CapturedVariableCheckerData(
    val context: CheckerContext,
    val reporter: DiagnosticReporter,
    val propertiesStack: MutableList<Set<FirPropertySymbol>> = mutableListOf(),
)

class FirFunctionDeepVisitorWithData : FirDefaultVisitor<Unit, CapturedVariableCheckerData>() {
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

    override fun visitVariableAssignment(variableAssignment: FirVariableAssignment, data: CapturedVariableCheckerData) {
        variableAssignment.rValue.accept(this, data)
    }

    override fun visitQualifiedAccessExpression(
        qualifiedAccessExpression: FirQualifiedAccessExpression,
        data: CapturedVariableCheckerData,
    ) {
        if (data.propertiesStack.isEmpty()) return
        val variableSymbol = qualifiedAccessExpression.calleeReference.toResolvedVariableSymbol() ?: return
        checkCapturedVariable(variableSymbol, data, qualifiedAccessExpression.source)
    }

    private fun checkCapturedVariable(variableSymbol: FirVariableSymbol<*>, data: CapturedVariableCheckerData, source: KtSourceElement?) {
        if (data.propertiesStack.isEmpty()) return
        if (variableSymbol.isVal) return
        if (variableSymbol.resolvedReturnType is ConeDynamicType) return
        if (!variableSymbol.isLocal) return
        if (variableSymbol in data.propertiesStack.last()) return
        val report = IEReporter(source, data.context, data.reporter, FirErrors.CV_DIAGNOSTIC)
        report(
            IEData(
                info = "Variable is captured from outer scope",
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