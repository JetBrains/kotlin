/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.cfa

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.contracts.description.isInPlace
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactory1
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.cfa.util.VariableInitializationInfoData
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.fir.declarations.InlineStatus
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.toResolvedVariableSymbol
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.QualifiedAccessNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.VariableAssignmentNode
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.fir.types.ConeDynamicType
import kotlin.reflect.full.memberProperties


object FirCapturedMutableVariablesAnalyzer : AbstractFirPropertyInitializationChecker(MppCheckerKind.Common) {
    @OptIn(SymbolInternals::class)
    context(reporter: DiagnosticReporter, context: CheckerContext)
    override fun analyze(data: VariableInitializationInfoData) {
        val containingLambda = data.graph.declaration as? FirAnonymousFunction ?: return
        val lambdaSymbol = containingLambda.symbol
        if (lambdaSymbol.inlineStatus == InlineStatus.Inline) return
        val invocationKind = containingLambda.invocationKind
        if (invocationKind.isInPlace) return
        for (node in data.graph.nodes) {
            val (expression, variableSymbol) = when (node) {
                is QualifiedAccessNode -> node.fir to (node.fir.calleeReference.toResolvedVariableSymbol() ?: continue)
                is VariableAssignmentNode -> node.fir to (node.fir.calleeReference?.toResolvedVariableSymbol() ?: continue)
                else -> continue
            }
            val source = expression.source ?: continue
            if (variableSymbol.isVal) continue

            if (variableSymbol.resolvedReturnType is ConeDynamicType) continue
            val accessExpression = when (expression) {
                is FirQualifiedAccessExpression -> expression
                is FirVariableAssignment -> {
                    expression.lValue as? FirQualifiedAccessExpression
                }
                else -> {
                    val report = IEReporter(source, context, reporter, FirErrors.CV_DIAGNOSTIC)
                    report(
                        IEData(
                            info = "Unexpected expression type for variable capture",
                            containingLambda = containingLambda.symbol.name.toString(),
                            variableName = variableSymbol.name.toString(),
                            leftmostReceiverName = "no receiver",
                        )
                    )
                    return
                }
            }

            if (!variableSymbol.isLocal) continue
            if (variableSymbol in data.properties) continue

            val report = IEReporter(source, context, reporter, FirErrors.CV_DIAGNOSTIC)
            report(
                IEData(
                    info = "Variable is captured from outer scope",
                    containingLambda = containingLambda.symbol.name.toString(),
                    variableName = variableSymbol.name.toString(),
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
    val containingLambda: String? = null,
    val variableName: String? = null,
    val leftmostReceiverName: String? = null,
)