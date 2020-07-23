/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.extended


import org.jetbrains.kotlin.contracts.description.EventOccurrencesRange
import org.jetbrains.kotlin.fir.FirFakeSourceElement
import org.jetbrains.kotlin.fir.analysis.cfa.*
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirBasicDeclarationChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.*
import org.jetbrains.kotlin.fir.resolve.dfa.controlFlowGraph
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.toFirPsiSourceElement
import org.jetbrains.kotlin.psi.KtProperty


object VariableAssignmentChecker : FirBasicDeclarationChecker() {
    override fun check(declaration: FirDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        if (declaration !is FirFunction<*>) return

        val graph = declaration.controlFlowGraphReference.controlFlowGraph ?: return
        val unprocessedProperties = mutableSetOf<FirPropertySymbol>()
        val propertiesCharacteristics = mutableMapOf<FirPropertySymbol, AssignmentsCount>()

        val visitor = ControlFlowGraphDeclarationVisitor(unprocessedProperties, propertiesCharacteristics)
        visitor.analyze(graph, reporter)

        for (property in unprocessedProperties) {
            if (property.fir.source is FirFakeSourceElement<*>) continue
            if (property.callableId.callableName.asString() == "<destruct>") continue
            propertiesCharacteristics[property] = AssignmentsCount.ZERO
        }

        for ((symbol, value) in propertiesCharacteristics) {
            val source = symbol.valOrVarOrJustKeywordSource
            when (value) {
                AssignmentsCount.ZERO -> {
                    reporter.report(source, FirErrors.UNINITIALIZED_VARIABLE_EXTENDED)
                }
                AssignmentsCount.ONE -> {
                    if (symbol.fir.isVar) {
                        reporter.report(source, FirErrors.CAN_BE_VAL)
                    }
                }
                else -> {
                }
            }
        }
    }

    private val FirPropertySymbol.valOrVarOrJustKeywordSource
        get() = (fir.psi as? KtProperty)?.valOrVarKeyword?.toFirPsiSourceElement() ?: fir.source

    enum class AssignmentsCount {
        ZERO,
        ONE,
        MANY
    }
}


class ControlFlowGraphDeclarationVisitor(
    private val unprocessedProperties: MutableSet<FirPropertySymbol>,
    private val propertiesCharacteristics: MutableMap<FirPropertySymbol, VariableAssignmentChecker.AssignmentsCount>
) : AbstractFirPropertyInitializationAnalyzer() {
    override fun analyze(graph: ControlFlowGraph, reporter: DiagnosticReporter) {
        val localProperties = LocalPropertyCollector.collect(graph)
        if (localProperties.isEmpty()) return

        val data = DataCollector(localProperties).getData(graph)
        val reporterVisitor = UninitializedPropertyReporter(data, localProperties, unprocessedProperties, propertiesCharacteristics)
        graph.traverse(TraverseDirection.Forward, reporterVisitor)
    }

    private class UninitializedPropertyReporter(
        val data: Map<CFGNode<*>, PropertyInitializationInfo>,
        val localProperties: Set<FirPropertySymbol>,
        val unprocessedProperties: MutableSet<FirPropertySymbol>,
        val propertiesCharacteristics: MutableMap<FirPropertySymbol, VariableAssignmentChecker.AssignmentsCount>
    ) : ControlFlowGraphVisitorVoid() {
        override fun visitNode(node: CFGNode<*>) {}

        override fun visitVariableAssignmentNode(node: VariableAssignmentNode) {
            val symbol = (node.fir.calleeReference as? FirResolvedNamedReference)?.resolvedSymbol as? FirPropertySymbol ?: return
            if (symbol !in localProperties) return
            unprocessedProperties.remove(symbol)

            val currentCharacteristic = propertiesCharacteristics.getOrDefault(symbol, VariableAssignmentChecker.AssignmentsCount.ZERO)

            when (data.getValue(node)[symbol] ?: EventOccurrencesRange.ZERO) {
                EventOccurrencesRange.AT_MOST_ONCE -> {
                    propertiesCharacteristics[symbol] = maxOf(currentCharacteristic, VariableAssignmentChecker.AssignmentsCount.ONE)
                }
                EventOccurrencesRange.EXACTLY_ONCE -> {
                    propertiesCharacteristics[symbol] = maxOf(currentCharacteristic, VariableAssignmentChecker.AssignmentsCount.ONE)
                }
                EventOccurrencesRange.AT_LEAST_ONCE, EventOccurrencesRange.MORE_THAN_ONCE -> {
                    propertiesCharacteristics[symbol] = VariableAssignmentChecker.AssignmentsCount.MANY
                }
                else -> {
                    propertiesCharacteristics[symbol] = maxOf(currentCharacteristic, VariableAssignmentChecker.AssignmentsCount.ZERO)
                }
            }
        }

        override fun visitVariableDeclarationNode(node: VariableDeclarationNode) {
            val symbol = node.fir.symbol
            if (node.fir.initializer == null && node.fir.delegate == null) {
                unprocessedProperties.add(symbol)
            } else {
                propertiesCharacteristics[symbol] = VariableAssignmentChecker.AssignmentsCount.ONE
            }
        }
    }

}