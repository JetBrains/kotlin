
package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.contracts.description.EventOccurrencesRange
import org.jetbrains.kotlin.contracts.description.isInPlace
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactory1
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.analysis.cfa.util.*
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.collectors.components.ControlFlowAnalysisDiagnosticComponent
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.toResolvedVariableSymbol
import org.jetbrains.kotlin.fir.render
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
object FirExactlyOncePopularityChecker : FirFunctionChecker(MppCheckerKind.Common) {
    @OptIn(SymbolInternals::class)
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirFunction) {
        if (context.containingElements.any { it is FirFunction && it != declaration }) return

        val visitor = FirFunctionDeepVisitorWithData2()
        visitor.visitFunction(
            declaration,
            CapturedVariableCheckerData(
                context,
                reporter,
            )
        )
    }

}

data class CapturedVariableCheckerData(
    val context: CheckerContext,
    val reporter: DiagnosticReporter,
    val propertiesStack: MutableList<Set<FirPropertySymbol>> = mutableListOf(),
    var currentWrite: FirQualifiedAccessExpression? = null
)

private class FirFunctionDeepVisitorWithData2 : FirDefaultVisitor<Unit, CapturedVariableCheckerData>() {
    override fun visitElement(element: FirElement, data: CapturedVariableCheckerData) {
        element.acceptChildren(this, data)
    }

    override fun visitAnonymousFunction(anonymousFunction: FirAnonymousFunction, data: CapturedVariableCheckerData) {
        val invocationKind = anonymousFunction.invocationKind
        if (invocationKind == EventOccurrencesRange.EXACTLY_ONCE) {
            println("anonymous function exactly once: ${anonymousFunction.render()}")
            val graph = (anonymousFunction as? FirControlFlowGraphOwner)?.controlFlowGraphReference?.controlFlowGraph ?: return

            val collector = ControlFlowAnalysisDiagnosticComponent.LocalPropertyCollector().apply {
                anonymousFunction.acceptChildren(this, graph.subGraphs.toSet())
            }
            data.propertiesStack.add(collector.properties)
        }
        super.visitAnonymousFunction(anonymousFunction, data)
        if (invocationKind == EventOccurrencesRange.EXACTLY_ONCE) {
            data.propertiesStack.removeLast()
        }
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

    private fun FirExpression.checkExpressionCapturedVariable(data: CapturedVariableCheckerData) {
        if (this is FirQualifiedAccessExpression) {
            val symbol = this.calleeReference.toResolvedVariableSymbol() ?: return
            if ((data.currentWrite != null && data.currentWrite == this)) {
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
        if (!variableSymbol.isVal) return
        if (variableSymbol.resolvedReturnType is ConeDynamicType) return
        if (!variableSymbol.isLocal) return
        val report = IEReporter(source, data.context, data.reporter, FirErrors.EO_DIAGNOSTIC)
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

