/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.fir.analysis.cfa.util.TraverseDirection
import org.jetbrains.kotlin.fir.analysis.cfa.util.collectDataForNode
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.reportOn
import org.jetbrains.kotlin.fir.declarations.FirAnonymousInitializer
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.expressions.FirVariableAssignment
import org.jetbrains.kotlin.fir.expressions.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.CFGNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ControlFlowGraphVisitor
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.EdgeLabel
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.VariableAssignmentNode
import org.jetbrains.kotlin.fir.resolve.dfa.controlFlowGraph
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

object FirPropertyInitializationChecker : FirRegularClassChecker() {
    override fun check(declaration: FirRegularClass, context: CheckerContext, reporter: DiagnosticReporter) {
        val properties = mutableSetOf<FirPropertySymbol>()
        val toReport = mutableSetOf<FirVariableAssignment>()
        for (decl in declaration.declarations.asReversed()) {
            when (decl) {
                is FirProperty -> properties.add(decl.symbol)
                is FirAnonymousInitializer ->
                    if (properties.isNotEmpty()) {
                        collectInitAssignments(decl, properties, toReport)
                    }
                else -> {
                }
            }
        }
        toReport.forEach { exp ->
            val propertySymbol = exp.lValue.toResolvedCallableSymbol() ?: return@forEach
            reporter.reportOn(exp.lValue.source, FirErrors.INITIALIZATION_BEFORE_DECLARATION, propertySymbol, context)
        }
    }

    private fun collectInitAssignments(
        init: FirAnonymousInitializer,
        properties: Properties,
        assignments: MutableSet<FirVariableAssignment>
    ) {
        val graph = init.controlFlowGraphReference?.controlFlowGraph ?: return
        val visitor = AssignmentVisitor(properties) { assignments.add(it) }
        graph.collectDataForNode(TraverseDirection.Forward, emptySet(), visitor, visitSubGraphs = false)
    }

    private class AssignmentVisitor(private val interestingProperties: Properties, private val acceptor: (FirVariableAssignment) -> Unit) :
        ControlFlowGraphVisitor<Properties, Collection<Pair<EdgeLabel, Properties>>>() {

        override fun visitNode(node: CFGNode<*>, data: Collection<Pair<EdgeLabel, Properties>>): Properties {
            if (data.isEmpty()) return emptySet()
            return data.map { it.second }.reduce { l, r -> l.intersect(r) }
        }

        override fun visitVariableAssignmentNode(node: VariableAssignmentNode, data: Collection<Pair<EdgeLabel, Properties>>): Properties {
            val input = visitNode(node, data)
            val propertySymbol = node.fir.lValue.toResolvedCallableSymbol().safeAs<FirPropertySymbol>() ?: return input
            return if (propertySymbol in interestingProperties && propertySymbol !in input) {
                acceptor(node.fir)
                input + propertySymbol
            } else {
                input
            }
        }
    }
}

private typealias Properties = Set<FirPropertySymbol>
