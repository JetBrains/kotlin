/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.extended


import org.jetbrains.kotlin.contracts.description.EventOccurrencesRange
import org.jetbrains.kotlin.fir.FirFakeSourceElement
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.analysis.cfa.AbstractFirPropertyInitializationChecker
import org.jetbrains.kotlin.fir.analysis.cfa.TraverseDirection
import org.jetbrains.kotlin.fir.analysis.cfa.traverse
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.*
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.toFirPsiSourceElement
import org.jetbrains.kotlin.psi.KtProperty


object VariableAssignmentChecker : AbstractFirPropertyInitializationChecker() {
    override fun analyze(
        graph: ControlFlowGraph,
        reporter: DiagnosticReporter,
        data: Map<CFGNode<*>, PropertyInitializationInfo>,
        properties: Set<FirPropertySymbol>
    ) {
        val unprocessedProperties = mutableSetOf<FirPropertySymbol>()
        val propertiesCharacteristics = mutableMapOf<FirPropertySymbol, EventOccurrencesRange>()

        val reporterVisitor = UninitializedPropertyReporter(data, properties, unprocessedProperties, propertiesCharacteristics)
        graph.traverse(TraverseDirection.Forward, reporterVisitor)

        for (property in unprocessedProperties) {
            if (property.fir.source is FirFakeSourceElement<*>) continue
            if (property.callableId.callableName.asString() == "<destruct>") continue
            propertiesCharacteristics[property] = EventOccurrencesRange.ZERO
        }

        var lastDestructuringSource: FirSourceElement? = null
        var destructuringCanBeVal = false
        var lastDestructuredVariables = 0

        for ((symbol, value) in propertiesCharacteristics) {
            val source = symbol.getValOrVarSource
            if (symbol.callableId.callableName.asString() == "<destruct>") {
                lastDestructuringSource = symbol.getValOrVarSource
                val childrenCount = symbol.fir.psi?.children?.size ?: continue
                lastDestructuredVariables = childrenCount - 1 // -1 cuz we don't need expression node after equals operator
                destructuringCanBeVal = true
                continue
            }

            if (lastDestructuringSource != null) {
                // if this is the last variable in destructuring declaration and destructuringCanBeVal == true and it can be val
                if (lastDestructuredVariables == 1 && destructuringCanBeVal && canBeVal(symbol, value)) {
                    reporter.report(lastDestructuringSource, FirErrors.CAN_BE_VAL)
                    lastDestructuringSource = null
                } else if (!canBeVal(symbol, value)) {
                    destructuringCanBeVal = false
                }
                lastDestructuredVariables--
            } else if (canBeVal(symbol, value) && symbol.fir.delegate == null ) {
                reporter.report(source, FirErrors.CAN_BE_VAL)
            }
        }
    }

    private fun canBeVal(symbol: FirPropertySymbol, value: EventOccurrencesRange) =
        (value == EventOccurrencesRange.EXACTLY_ONCE
                || value == EventOccurrencesRange.AT_MOST_ONCE
                || value == EventOccurrencesRange.ZERO
                ) && symbol.fir.isVar

    private class UninitializedPropertyReporter(
        val data: Map<CFGNode<*>, PropertyInitializationInfo>,
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
            propertiesCharacteristics[symbol] = currentCharacteristic.or(data.getValue(node)[symbol] ?: EventOccurrencesRange.ZERO)
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

    private val FirPropertySymbol.getValOrVarSource
        get() = (fir.psi as? KtProperty)?.valOrVarKeyword?.toFirPsiSourceElement()
            ?: fir.psi?.firstChild?.toFirPsiSourceElement()
            ?: fir.source
}