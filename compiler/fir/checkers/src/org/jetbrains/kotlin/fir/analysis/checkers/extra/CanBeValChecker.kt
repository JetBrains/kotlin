/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.extra

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.contracts.description.canBeRevisited
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.cfa.AbstractFirPropertyInitializationChecker
import org.jetbrains.kotlin.fir.analysis.cfa.nearestNonInPlaceGraph
import org.jetbrains.kotlin.fir.analysis.cfa.requiresInitialization
import org.jetbrains.kotlin.fir.analysis.cfa.util.VariableInitializationInfoData
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.utils.isLateInit
import org.jetbrains.kotlin.fir.expressions.calleeReference
import org.jetbrains.kotlin.fir.references.toResolvedPropertySymbol
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.*
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.util.getChildren

object CanBeValChecker : AbstractFirPropertyInitializationChecker(MppCheckerKind.Common) {
    override fun analyze(data: VariableInitializationInfoData, reporter: DiagnosticReporter, context: CheckerContext) {
        val isForInitialization = data.graph.kind == ControlFlowGraph.Kind.Class || data.graph.kind == ControlFlowGraph.Kind.File
        val collector = ReassignedVariableCollector(data, isForInitialization).apply { data.graph.traverse(this) }
        val iterator = data.properties.iterator()
        for (symbol in iterator) {
            val source = symbol.source ?: continue
            val canBeVal = if (source.elementType == KtNodeTypes.DESTRUCTURING_DECLARATION) {
                // var (a, b) -> { val _tmp; var a; var b }
                val count = source.lighterASTNode.getChildren(source.treeStructure).count {
                    it.tokenType == KtNodeTypes.DESTRUCTURING_DECLARATION_ENTRY
                }
                // Weird way of writing `all { ... }` that will always call `next()` N times.
                (0 until count).fold(true) { acc, _ -> iterator.hasNext() && collector.canBeVal(iterator.next()) && acc }
            } else {
                collector.canBeVal(symbol)
            }
            if (canBeVal) {
                val diagnostic = when {
                    symbol is FirPropertySymbol && !symbol.requiresInitialization(isForInitialization) -> FirErrors.CAN_BE_VAL
                    symbol.isLateInit -> FirErrors.CAN_BE_VAL_LATEINIT
                    else -> FirErrors.CAN_BE_VAL_DELAYED_INITIALIZATION
                }
                reporter.reportOn(source, diagnostic, context)
            }
        }
    }

    private class ReassignedVariableCollector(
        private val data: VariableInitializationInfoData,
        private val isForInitialization: Boolean,
    ) : ControlFlowGraphVisitorVoid() {
        private val declaredIn = mutableMapOf<FirPropertySymbol, ControlFlowGraph>()
        private val reassigned = mutableSetOf<FirPropertySymbol>()

        override fun visitNode(node: CFGNode<*>) {}

        override fun visitVariableDeclarationNode(node: VariableDeclarationNode) {
            declaredIn[node.fir.symbol] = node.owner.nearestNonInPlaceGraph()
        }

        override fun visitVariableAssignmentNode(node: VariableAssignmentNode) {
            val symbol = node.fir.calleeReference?.toResolvedPropertySymbol() ?: return
            if (symbol.isVar && symbol.source?.kind !is KtFakeSourceElementKind && symbol in data.properties) {
                val isReassigned = !symbol.requiresInitialization(isForInitialization) ||
                        data.getValue(node).values.any { it[symbol]?.canBeRevisited() == true } ||
                        declaredIn[symbol] != node.owner.nearestNonInPlaceGraph()
                if (isReassigned) reassigned.add(symbol)
            }
        }

        fun canBeVal(symbol: FirVariableSymbol<*>): Boolean {
            require(symbol is FirPropertySymbol)
            return symbol.isVar && !symbol.hasDelegate && symbol.source?.kind !is KtFakeSourceElementKind? && symbol !in reassigned
        }
    }
}
