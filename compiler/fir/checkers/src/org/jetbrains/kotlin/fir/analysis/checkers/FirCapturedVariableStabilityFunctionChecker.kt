/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.cfa.AbstractFirPropertyInitializationChecker
import org.jetbrains.kotlin.fir.analysis.cfa.nearestNonInPlaceGraph
import org.jetbrains.kotlin.fir.analysis.cfa.util.*
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirImplicitInvokeCall
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.config.AnalysisFlags
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.references.toResolvedFunctionSymbol
import org.jetbrains.kotlin.fir.references.toResolvedVariableSymbol
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.*
import org.jetbrains.kotlin.fir.symbols.impl.FirAnonymousFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirLocalPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.types.ConeDynamicType

/**
 * Checks captured variables inside non-in-place lambdas and determines their stability
 * using [FindCapturedWrites, FindVisibleWrites].
 */
object FirCapturedVariableStabilityFunctionChecker : AbstractFirPropertyInitializationChecker(MppCheckerKind.Common) {
    context(reporter: DiagnosticReporter, context: CheckerContext)
    override fun analyze(data: VariableInitializationInfoData) {
        if (!context.session.languageVersionSettings.supportsFeature(LanguageFeature.ReportEscapingCapturedVariable)) return

        val trackedProperties = data.properties
            .filterIsInstance<FirLocalPropertySymbol>()
            .filterTo(linkedSetOf()) { symbol ->
                !symbol.isVal &&
                        symbol.resolvedReturnType !is ConeDynamicType
            }
        if (trackedProperties.isEmpty()) return

        val lambdaOwnerCalls = mutableMapOf<FirAnonymousFunctionSymbol, FirFunctionCall>()

        data.graph.traverse(object : ControlFlowGraphVisitorVoid() {
            override fun visitNode(node: CFGNode<*>) {}

            override fun visitSplitPostponedLambdasNode(node: SplitPostponedLambdasNode) {
                val call = node.fir as? FirFunctionCall ?: return

                for (lambda in node.lambdas) {
                    lambdaOwnerCalls[lambda.symbol] = call
                }
            }
        })

        val capturedWrites = data.graph.traverseToFixedPoint(FindCapturedWrites(trackedProperties))
        val visibleWrites =
            data.graph.traverseToFixedPoint(FindVisibleWrites(capturedWrites, trackedProperties, excludeLocalInPlaceWrites = true))

        val propertyDeclarationGraphs = mutableMapOf<FirPropertySymbol, ControlFlowGraph>()
        data.graph.traverse(object : ControlFlowGraphVisitorVoid() {
            override fun visitNode(node: CFGNode<*>) {}
            override fun visitVariableDeclarationExitNode(node: VariableDeclarationExitNode) {
                val symbol = node.fir.symbol
                if (symbol in trackedProperties) {
                    propertyDeclarationGraphs[symbol] = node.owner.nearestNonInPlaceGraph()
                }
            }
        })

        val userEntries = context.session.languageVersionSettings.getFlag(AnalysisFlags.escapingFunctionsList)
        val added = userEntries.filter { it.startsWith("+") }.map { it.drop(1) }
        val removed = userEntries.filter { it.startsWith("-") }.map { it.drop(1) }
        val effectiveEscapingFunctionsList = defaultEscapingFunctionsList + added - removed.toSet()

        data.graph.traverse(
            CapturedVariableVisitor(
                visibleWrites,
                propertyDeclarationGraphs,
                lambdaOwnerCalls,
                effectiveEscapingFunctionsList,
                reporter,
                context,
            )
        )
    }
}

private class CapturedVariableVisitor(
    private val visibleWrites: Map<CFGNode<*>, PathAwareControlFlowInfo<PropertyAccessType, VariableWriteData>>,
    private val propertyDeclarationGraphs: Map<FirPropertySymbol, ControlFlowGraph>,
    private val lambdaOwnerCalls: Map<FirAnonymousFunctionSymbol, FirFunctionCall>,
    private val escapingFunctionsList: Set<String>,
    private val reporter: DiagnosticReporter,
    private val context: CheckerContext,
) : ControlFlowGraphVisitorVoid() {


    private fun isAllowlistedDispatcher(call: FirFunctionCall): Boolean {
        val symbol = call.calleeReference.toResolvedFunctionSymbol() ?: return false
        return symbol.callableId.asSingleFqName().asString() in escapingFunctionsList
    }

    override fun visitNode(node: CFGNode<*>) {}

    override fun visitQualifiedAccessNode(node: QualifiedAccessNode) {
        reportIfNeeded(node, node.fir)
    }

    override fun visitFunctionCallEnterNode(node: FunctionCallEnterNode) {
        // TODO(KT-76534): remove when implicit invoke is handled in CFG correctly.
        val call = node.fir as? FirImplicitInvokeCall ?: return
        val receiver = (call.dispatchReceiver ?: call.explicitReceiver) as? FirQualifiedAccessExpression ?: return
        val receiverExitNode =
            (node.firstPreviousNode as? FunctionCallArgumentsExitNode)?.explicitReceiverExitNode ?: return

        reportIfNeeded(receiverExitNode, receiver)
    }

    private fun reportIfNeeded(
        accessNode: CFGNode<*>,
        expression: FirQualifiedAccessExpression,
    ) {
        val currentGraph = accessNode.owner.nearestNonInPlaceGraph()
        val currentLambda = currentGraph.declaration as? FirAnonymousFunction ?: return
        val ownerCall = lambdaOwnerCalls[currentLambda.symbol] ?: return
        if (!isAllowlistedDispatcher(ownerCall)) return

        val symbol = expression.calleeReference.toResolvedVariableSymbol() as? FirPropertySymbol ?: return

        // Only report if the variable is tracked and captured
        val declarationGraph = propertyDeclarationGraphs[symbol]
        if (declarationGraph == null || declarationGraph == currentGraph) {
            return
        }

        val hasCapturedWrites = visibleWrites[accessNode]
            ?.values
            ?.any { controlFlowInfo ->
                controlFlowInfo.values.any { writeData ->
                    writeData[symbol]?.isNotEmpty() == true
                }
            } == true

        if (hasCapturedWrites) {
            reporter.reportOn(
                expression.source,
                FirErrors.ESCAPING_CAPTURED_VARIABLE,
                symbol,
                context
            )
        }
    }
}

private val defaultEscapingFunctionsList: Set<String> = setOf(
    "kotlinx.coroutines.launch",
    "kotlinx.coroutines.async",
    "kotlinx.coroutines.Job.invokeOnCompletion",
    "javax.swing.SwingUtilities.invokeLater",
    "java.awt.EventQueue.invokeLater",
    "com.intellij.openapi.application.Application.invokeLater",
    "java.util.concurrent.Executor.execute",
    "java.util.concurrent.ExecutorService.submit",
    // Constructor with escaping functional argument.
    "java.lang.Thread.Thread",
)
