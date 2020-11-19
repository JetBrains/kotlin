/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.extended

import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.contracts.description.EventOccurrencesRange
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.analysis.cfa.*
import org.jetbrains.kotlin.fir.analysis.checkers.getChildren
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.getChild
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.*
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.lexer.KtTokens

object CanBeValChecker : AbstractFirPropertyInitializationChecker() {
    override fun analyze(
        graph: ControlFlowGraph,
        reporter: DiagnosticReporter,
        data: Map<CFGNode<*>, PathAwarePropertyInitializationInfo>,
        properties: Set<FirPropertySymbol>
    ) {
        val unprocessedProperties = mutableSetOf<FirPropertySymbol>()
        val propertiesCharacteristics = mutableMapOf<FirPropertySymbol, EventOccurrencesRange>()

        val reporterVisitor = UninitializedPropertyReporter(data, properties, unprocessedProperties, propertiesCharacteristics)
        graph.traverse(TraverseDirection.Forward, reporterVisitor)

        for (property in unprocessedProperties) {
            if (property.fir.source is FirFakeSourceElement<*>) continue
            if (property.isDestructuring) continue
            propertiesCharacteristics[property] = EventOccurrencesRange.ZERO
        }

        var lastDestructuringSource: FirSourceElement? = null
        var destructuringCanBeVal = false
        var lastDestructuredVariables = 0

        for ((symbol, value) in propertiesCharacteristics) {
            val source = symbol.fir.source?.getChild(setOf(KtTokens.VAL_KEYWORD, KtTokens.VAR_KEYWORD), depth = 1)
            if (symbol.isDestructuring) {
                lastDestructuringSource = source
                lastDestructuredVariables = symbol.getDestructuringChildrenCount() ?: continue
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
            } else if (canBeVal(symbol, value) && symbol.fir.delegate == null) {
                reporter.report(source, FirErrors.CAN_BE_VAL)
            }
        }
    }

    private fun canBeVal(symbol: FirPropertySymbol, value: EventOccurrencesRange) =
        value in canBeValOccurrenceRanges && symbol.fir.isVar

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

    private fun FirPropertySymbol.getDestructuringChildrenCount(): Int? = when (fir.source) {
        is FirPsiSourceElement<*> -> fir.psi?.children?.size?.minus(1) // -1 cuz we don't need expression node after equals operator
        is FirLightSourceElement -> {
            val source = fir.source as FirLightSourceElement
            val tree = (fir.source as FirLightSourceElement).treeStructure
            val children = source.lighterASTNode.getChildren(tree)
            children.filter { it?.tokenType == KtNodeTypes.DESTRUCTURING_DECLARATION_ENTRY }.size
        }
        else -> null
    }

    private val FirPropertySymbol.isDestructuring
        get() = callableId.callableName.asString() == "<destruct>"

    private val canBeValOccurrenceRanges = setOf(
        EventOccurrencesRange.EXACTLY_ONCE,
        EventOccurrencesRange.AT_MOST_ONCE,
        EventOccurrencesRange.ZERO
    )
}