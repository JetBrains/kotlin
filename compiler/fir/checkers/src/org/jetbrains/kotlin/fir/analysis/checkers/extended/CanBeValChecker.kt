/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.extended

import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.contracts.description.EventOccurrencesRange
import org.jetbrains.kotlin.fir.FirFakeSourceElement
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.analysis.cfa.AbstractFirPropertyInitializationChecker
import org.jetbrains.kotlin.fir.analysis.cfa.util.PathAwarePropertyInitializationInfo
import org.jetbrains.kotlin.fir.analysis.cfa.util.TraverseDirection
import org.jetbrains.kotlin.fir.analysis.cfa.util.traverse
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.getChildren
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.reportOn
import org.jetbrains.kotlin.fir.expressions.FirVariableAssignment
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.*
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol

object CanBeValChecker : AbstractFirPropertyInitializationChecker() {
    override fun analyze(
        graph: ControlFlowGraph,
        reporter: DiagnosticReporter,
        data: Map<CFGNode<*>, PathAwarePropertyInitializationInfo>,
        properties: Set<FirPropertySymbol>,
        capturedWrites: Set<FirVariableAssignment>,
        context: CheckerContext
    ) {
        val unprocessedProperties = mutableSetOf<FirPropertySymbol>()
        val propertiesCharacteristics = mutableMapOf<FirPropertySymbol, EventOccurrencesRange>()

        val reporterVisitor = UninitializedPropertyReporter(data, properties, unprocessedProperties, propertiesCharacteristics)
        graph.traverse(TraverseDirection.Forward, reporterVisitor)

        for (property in unprocessedProperties) {
            val source = property.source
            if (source is FirFakeSourceElement) continue
            if (source?.elementType == KtNodeTypes.DESTRUCTURING_DECLARATION) continue
            propertiesCharacteristics[property] = EventOccurrencesRange.ZERO
        }

        var lastDestructuringSource: FirSourceElement? = null
        var destructuringCanBeVal = false
        var lastDestructuredVariables = 0

        for ((symbol, value) in propertiesCharacteristics) {
            val source = symbol.source
            if (source?.elementType == KtNodeTypes.DESTRUCTURING_DECLARATION) {
                lastDestructuringSource = source
                lastDestructuredVariables = symbol.getDestructuringChildrenCount() ?: continue
                destructuringCanBeVal = true
                continue
            }

            if (lastDestructuringSource != null) {
                // if this is the last variable in destructuring declaration and destructuringCanBeVal == true and it can be val
                if (lastDestructuredVariables == 1 && destructuringCanBeVal && canBeVal(symbol, value)) {
                    reporter.reportOn(lastDestructuringSource, FirErrors.CAN_BE_VAL, context)
                    lastDestructuringSource = null
                } else if (!canBeVal(symbol, value)) {
                    destructuringCanBeVal = false
                }
                lastDestructuredVariables--
            } else if (canBeVal(symbol, value) && !symbol.hasDelegate) {
                reporter.reportOn(source, FirErrors.CAN_BE_VAL, context)
            }
        }
    }

    private fun canBeVal(symbol: FirPropertySymbol, value: EventOccurrencesRange) =
        value in canBeValOccurrenceRanges && symbol.isVar

    private class UninitializedPropertyReporter(
        val data: Map<CFGNode<*>, PathAwarePropertyInitializationInfo>,
        val localProperties: Set<FirPropertySymbol>,
        val unprocessedProperties: MutableSet<FirPropertySymbol>,
        val propertiesCharacteristics: MutableMap<FirPropertySymbol, EventOccurrencesRange>
    ) : ControlFlowGraphVisitorVoid() {
        override fun visitNode(node: CFGNode<*>) {}

        override fun visitVariableAssignmentNode(node: VariableAssignmentNode) {
            val symbol = (node.fir.calleeReference as? FirResolvedNamedReference)?.resolvedSymbol as? FirPropertySymbol
                ?: return
            if (symbol !in localProperties) return
            unprocessedProperties.remove(symbol)

            val currentCharacteristic = propertiesCharacteristics.getOrDefault(symbol, EventOccurrencesRange.ZERO)
            val info = data.getValue(node)
            propertiesCharacteristics[symbol] = currentCharacteristic.or(info.infoAtNormalPath[symbol] ?: EventOccurrencesRange.ZERO)
        }

        override fun visitVariableDeclarationNode(node: VariableDeclarationNode) {
            val symbol = node.fir.symbol
            if (node.fir.initializer == null && node.fir.delegate == null) {
                unprocessedProperties.add(symbol)
            } else {
                propertiesCharacteristics[symbol] = EventOccurrencesRange.AT_MOST_ONCE
            }
        }
    }

    private fun FirPropertySymbol.getDestructuringChildrenCount(): Int? {
        val source = source ?: return null
        return source.lighterASTNode.getChildren(source.treeStructure).count {
            it?.tokenType == KtNodeTypes.DESTRUCTURING_DECLARATION_ENTRY
        }
    }

    private val canBeValOccurrenceRanges = setOf(
        EventOccurrencesRange.EXACTLY_ONCE,
        EventOccurrencesRange.AT_MOST_ONCE,
        EventOccurrencesRange.ZERO
    )
}
