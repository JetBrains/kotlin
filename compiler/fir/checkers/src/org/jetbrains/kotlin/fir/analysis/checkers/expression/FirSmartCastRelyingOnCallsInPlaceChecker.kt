/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.contracts.description.EventOccurrencesRange
import org.jetbrains.kotlin.contracts.description.isInPlace
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactory1
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.cfa.util.FindCapturedWrites
import org.jetbrains.kotlin.fir.analysis.cfa.util.FindVisibleWrites
import org.jetbrains.kotlin.fir.analysis.cfa.util.PathAwareControlFlowInfo
import org.jetbrains.kotlin.fir.analysis.cfa.util.PropertyAccessType
import org.jetbrains.kotlin.fir.analysis.cfa.util.VariableWriteData
import org.jetbrains.kotlin.fir.analysis.cfa.util.traverseToFixedPoint
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirFunctionChecker
import org.jetbrains.kotlin.fir.analysis.collectors.components.ControlFlowAnalysisDiagnosticComponent
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.fir.declarations.FirControlFlowGraphOwner
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.utils.isInline
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.toResolvedVariableSymbol
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.CFGNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ControlFlowGraph
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.VariableAssignmentNode
import org.jetbrains.kotlin.fir.resolve.dfa.controlFlowGraph
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.fir.types.ConeDynamicType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.fir.types.typeContext
import org.jetbrains.kotlin.fir.visitors.FirDefaultVisitor
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.reflect.full.memberProperties

object FirSmartCastRelyingOnCallsInPlaceChecker : FirFunctionChecker(MppCheckerKind.Common) {
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
                visibleWrites
            )
        )
    }

    data class CapturedVariableCheckerData(
        val context: CheckerContext,
        val reporter: DiagnosticReporter,
        val visibleWrites: Map<CFGNode<*>, PathAwareControlFlowInfo<PropertyAccessType, VariableWriteData>>,
        val propertiesStack: MutableList<Pair<Set<FirPropertySymbol>, FirAnonymousFunction>> = mutableListOf(),
    )


    private class FirFunctionDeepVisitorWithData2 : FirDefaultVisitor<Unit, CapturedVariableCheckerData>() {
        override fun visitElement(element: FirElement, data: CapturedVariableCheckerData) {
            element.acceptChildren(this, data)
        }

        override fun visitAnonymousFunction(anonymousFunction: FirAnonymousFunction, data: CapturedVariableCheckerData) {
            val invocationKind = anonymousFunction.invocationKind
            val hasCallInPlaceContract = invocationKind != null && invocationKind.isInPlace

            val graph = (anonymousFunction as? FirControlFlowGraphOwner)?.controlFlowGraphReference?.controlFlowGraph ?: return

            val collector = ControlFlowAnalysisDiagnosticComponent.LocalPropertyCollector().apply {
                anonymousFunction.acceptChildren(this, graph.subGraphs.toSet())
            }

            if (hasCallInPlaceContract) {
                data.propertiesStack.add(Pair(collector.properties, anonymousFunction))
            }
            super.visitAnonymousFunction(anonymousFunction, data)
            if (hasCallInPlaceContract) {
                data.propertiesStack.removeLast()
            }
        }

        override fun visitSmartCastExpression(smartCastExpression: FirSmartCastExpression, data: CapturedVariableCheckerData) {
            if (!smartCastExpression.isStable) return
            smartCastExpression.originalExpression.checkExpressionCapturedVariable(data, smartCastExpression.smartcastType.coneType)
        }

        private fun isHasWriteFromNestedNode(
            pathInfo: PathAwareControlFlowInfo<PropertyAccessType, VariableWriteData>?,
            propertySymbol: FirPropertySymbol,
            smartCastType: ConeKotlinType,
            data: CapturedVariableCheckerData
        ): Boolean {
            // Check if there are Captured writes from different lambda contexts
            // We want to warn only if writes are in nested lambdas, not if they're in parent scope before the current lambda
            return pathInfo?.values?.any { controlFlowInfo ->
                controlFlowInfo[PropertyAccessType.Captured]?.get(propertySymbol)?.any { writeNode ->
                    if (writeNode is VariableAssignmentNode) {
                        val assignedValue = writeNode.fir.rValue
                        val assignedType = assignedValue.resolvedType
                        // If the assigned type is NOT a subtype of the smart cast type, report it
                        !AbstractTypeChecker.isSubtypeOf(data.context.session.typeContext, assignedType, smartCastType)
                    } else {
                        false
                    }
                } == true
            } == true

        }

        private fun isHasWrites(
            statement: FirStatement,
            data: CapturedVariableCheckerData,
            propertySymbol: FirPropertySymbol,
            smartCastType: ConeKotlinType
        ): Boolean {
            val accessNode = data.visibleWrites.keys.find { node ->
                node.fir == statement
            }
            if (accessNode == null) return false

            val pathInfo = accessNode.let { data.visibleWrites[it] }
            val hasCapturedWritesFromDifferentLambda = isHasWriteFromNestedNode(pathInfo, propertySymbol, smartCastType, data)
            return hasCapturedWritesFromDifferentLambda
        }

        private fun FirExpression.checkExpressionCapturedVariable(data: CapturedVariableCheckerData, smartCastType: ConeKotlinType) {
            if (this is FirQualifiedAccessExpression) {
                val symbol = this.calleeReference.toResolvedVariableSymbol() as? FirPropertySymbol ?: return
                val hasWrites = isHasWrites(this, data, symbol, smartCastType)
                if (hasWrites) {
                    checkCapturedVariable(symbol, data, this.source)
                }
                val receiver = this.explicitReceiver?.unwrapErrorExpression()?.unwrapArgument()
                receiver?.checkExpressionCapturedVariable(data, smartCastType)
            }
            if (this is FirCheckNotNullCall) {
                this.argument.checkExpressionCapturedVariable(data, smartCastType)
            }
            if (this is FirSafeCallExpression) {
                this.receiver.checkExpressionCapturedVariable(data, smartCastType)
            }
        }


        @OptIn(SymbolInternals::class)
        private fun checkCapturedVariable(
            variableSymbol: FirVariableSymbol<*>,
            data: CapturedVariableCheckerData,
            source: KtSourceElement?,
        ) {
            if (data.propertiesStack.isEmpty()) return
            if (variableSymbol.isVal) return
            val functionLocals = data.propertiesStack.last().first
            val anonymousFunction = data.propertiesStack.last().second
            if (variableSymbol in functionLocals) return
            if (variableSymbol.resolvedReturnType is ConeDynamicType) return
            if (!variableSymbol.isLocal) return
            val report = IEReporter(source, data.context, data.reporter, FirErrors.SMARTCAST_RELYING_ON_CALLS_IN_PLACE)
            report(
                IEData(
                    info = "",
                    variableName = variableSymbol.name.asString(),
                    resolvedType = variableSymbol.resolvedReturnType.toString(),
                    callsInPlaceType = anonymousFunction.invocationKind?.name.toString(),
                    inlineStatus = anonymousFunction.inlineStatus.toString(),
                    isInlineAndUnknown = (anonymousFunction.isInline && anonymousFunction.invocationKind == EventOccurrencesRange.UNKNOWN).toString()
                )
            )
        }
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
    val resolvedType: String? = null,
    val callsInPlaceType: String? = null,
    val inlineStatus: String? = null,
    val isInlineAndUnknown: String? = null,
)