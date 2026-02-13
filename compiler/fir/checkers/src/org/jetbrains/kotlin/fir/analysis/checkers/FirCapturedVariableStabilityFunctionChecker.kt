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
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.toResolvedPropertySymbol
import org.jetbrains.kotlin.fir.references.toResolvedVariableSymbol
import org.jetbrains.kotlin.fir.resolve.dfa.FirLocalVariableAssignmentAnalyzer
import org.jetbrains.kotlin.fir.resolve.dfa.controlFlowGraph
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.fir.types.ConeDynamicType
import org.jetbrains.kotlin.fir.types.resolvedType
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
        if (context.containingElements.any { it is FirFunction && it != declaration }) return
        val analyzer = FirLocalVariableAssignmentAnalyzer()
        val visitor = FirFunctionDeepVisitorWithData()
        visitor.visitFunction(
            declaration,
            CapturedVariableCheckerData(
                context,
                reporter,
                analyzer = analyzer,
            )
        )
    }
}

data class CapturedVariableCheckerData(
    val context: CheckerContext,
    val reporter: DiagnosticReporter,
    val propertiesStack: MutableList<Set<FirPropertySymbol>> = mutableListOf(),
    val analyzer: FirLocalVariableAssignmentAnalyzer,
    val visitedAnonymousFunctions: MutableSet<FirAnonymousFunction> = mutableSetOf(),
)

class FirFunctionDeepVisitorWithData : FirDefaultVisitor<Unit, CapturedVariableCheckerData>() {
    override fun visitElement(element: FirElement, data: CapturedVariableCheckerData) {
        element.acceptChildren(this, data)
    }


    override fun visitFunction(function: FirFunction, data: CapturedVariableCheckerData) {
        data.analyzer.enterFunction(function)
        super.visitFunction(function, data)
        data.analyzer.exitFunction()
    }

    override fun visitAnonymousFunction(anonymousFunction: FirAnonymousFunction, data: CapturedVariableCheckerData) {
        if (data.visitedAnonymousFunctions.contains(anonymousFunction)) return
        data.visitedAnonymousFunctions.add(anonymousFunction)
        data.analyzer.enterFunction(anonymousFunction)

        try {
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
        } finally {
            data.analyzer.exitFunction()
        }

    }

    // -------------------------
    // Assignments
    // -------------------------

    @OptIn(SymbolInternals::class)
    override fun visitVariableAssignment(variableAssignment: FirVariableAssignment, data: CapturedVariableCheckerData) {
        super.visitVariableAssignment(variableAssignment, data)

        if (variableAssignment.explicitReceiver != null) return
        val property = variableAssignment.calleeReference
            ?.toResolvedPropertySymbol()
            ?.fir
            ?: return

        data.analyzer.visitAssignment(property, variableAssignment.rValue.resolvedType)
    }

    override fun visitQualifiedAccessExpression(
        qualifiedAccessExpression: FirQualifiedAccessExpression,
        data: CapturedVariableCheckerData,
    ) {
        qualifiedAccessExpression.checkExpressionCapturedVariable(data)
    }

    // -------------------------
    // Top-level scopes
    // -------------------------

    override fun visitAnonymousInitializer(anonymousInitializer: FirAnonymousInitializer, data: CapturedVariableCheckerData) {
        data.analyzer.enterAnonymousInitializer(anonymousInitializer)
        try {
            super.visitAnonymousInitializer(anonymousInitializer, data)
        } finally {
            data.analyzer.exitAnonymousInitializer(anonymousInitializer)
        }
    }

    override fun visitCodeFragment(codeFragment: FirCodeFragment, data: CapturedVariableCheckerData) {
        data.analyzer.enterCodeFragment(codeFragment)
        try {
            super.visitCodeFragment(codeFragment, data)
        } finally {
            data.analyzer.exitCodeFragment(codeFragment)
        }
    }

    override fun visitReplSnippet(replSnippet: FirReplSnippet, data: CapturedVariableCheckerData) {
        data.analyzer.enterReplSnippet(replSnippet)
        try {
            super.visitReplSnippet(replSnippet, data)
        } finally {
            data.analyzer.exitReplSnippet(replSnippet)
        }
    }

    override fun visitClass(klass: FirClass, data: CapturedVariableCheckerData) {
        data.analyzer.enterClass(klass)
        try {
            super.visitClass(klass, data)
        } finally {
            data.analyzer.exitClass()
        }
    }

    // -------------------------
    // Loops
    // -------------------------

    override fun visitWhileLoop(
        whileLoop: FirWhileLoop,
        data: CapturedVariableCheckerData,
    ) {
        data.analyzer.enterLoop(whileLoop)
        try {
            super.visitWhileLoop(whileLoop, data)
        } finally {
            data.analyzer.exitLoop()
        }
    }

    override fun visitDoWhileLoop(
        doWhileLoop: FirDoWhileLoop,
        data: CapturedVariableCheckerData,
    ) {
        data.analyzer.enterLoop(doWhileLoop)
        try {
            super.visitDoWhileLoop(doWhileLoop, data)
        } finally {
            data.analyzer.exitLoop()
        }
    }

    // -------------------------
    // Function calls
    // -------------------------

    override fun visitFunctionCall(
        functionCall: FirFunctionCall,
        data: CapturedVariableCheckerData,
    ) {
        val lambdaArgs = functionCall.argumentList.arguments
            .asSequence()
            .filterIsInstance<FirAnonymousFunctionExpression>()
            .map { it.anonymousFunction }
            .toList()

        data.analyzer.enterFunctionCall(lambdaArgs)

        var completed = false
        try {
            super.visitFunctionCall(functionCall, data)
            completed = true
        } finally {
            data.analyzer.exitFunctionCall(callCompleted = completed)
        }
    }

    private fun FirExpression.checkExpressionCapturedVariable(data: CapturedVariableCheckerData) {
        if (this is FirQualifiedAccessExpression) {
            val symbol = this.calleeReference.toResolvedVariableSymbol() ?: return
            checkCapturedVariable(symbol, data, this.source)
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
        val declaration = variableSymbol.fir as? FirDeclaration ?: return
        val isUnstable = data.analyzer.isUnstableInCurrentScopeWithoutPreservingType(declaration)
        if (!isUnstable) return
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