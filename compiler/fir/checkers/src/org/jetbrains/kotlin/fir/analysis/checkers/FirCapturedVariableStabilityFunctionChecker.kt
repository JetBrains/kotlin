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
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirFunctionChecker
import org.jetbrains.kotlin.fir.analysis.collectors.components.ControlFlowAnalysisDiagnosticComponent
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.fir.declarations.FirControlFlowGraphOwner
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.InlineStatus
import org.jetbrains.kotlin.fir.expressions.calleeReference
import org.jetbrains.kotlin.fir.references.toResolvedVariableSymbol
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.QualifiedAccessNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.VariableAssignmentNode
import org.jetbrains.kotlin.fir.resolve.dfa.controlFlowGraph
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.types.ConeDynamicType
import kotlin.reflect.full.memberProperties

/**
 * Checks captured variables inside non-in-place lambdas and determines their stability
 * using [FirLocalVariableAssignmentAnalyzer].
 */
object FirCapturedVariableStabilityFunctionChecker : FirFunctionChecker(MppCheckerKind.Common) {
    @OptIn(SymbolInternals::class)
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirFunction) {
        if (declaration !is FirAnonymousFunction) return
        val lambdaSymbol = declaration.symbol
        if (lambdaSymbol.inlineStatus == InlineStatus.Inline) return
        val invocationKind = declaration.invocationKind
        if (invocationKind.isInPlace) return

        val graph = (declaration as? FirControlFlowGraphOwner)?.controlFlowGraphReference?.controlFlowGraph ?: return

        val collector = ControlFlowAnalysisDiagnosticComponent.LocalPropertyCollector().apply {
            declaration.acceptChildren(this, graph.subGraphs.toSet())
        }
        val properties = collector.properties


        for (node in graph.nodes) {
            val (expression, variableSymbol) = when (node) {
                is QualifiedAccessNode -> node.fir to (node.fir.calleeReference.toResolvedVariableSymbol() ?: continue)
                is VariableAssignmentNode -> node.fir to (node.fir.calleeReference?.toResolvedVariableSymbol() ?: continue)
                else -> continue
            }
            val source = expression.source ?: continue
            if (variableSymbol.isVal) continue

            if (variableSymbol.resolvedReturnType is ConeDynamicType) continue

            if (!variableSymbol.isLocal) continue
            if (variableSymbol in properties) continue

            val report = IEReporter(source, context, reporter, FirErrors.CV_DIAGNOSTIC)
            report(
                IEData(
                    info = "Variable is captured from outer scope",
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
    val variableName: String? = null,
    val leftmostReceiverName: String? = null,
)