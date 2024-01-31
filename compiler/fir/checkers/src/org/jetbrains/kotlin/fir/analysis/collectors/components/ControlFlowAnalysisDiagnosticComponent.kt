/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.collectors.components

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.cfa.util.PropertyInitializationInfoData
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.DeclarationCheckers
import org.jetbrains.kotlin.fir.analysis.checkersComponent
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.FirDoWhileLoop
import org.jetbrains.kotlin.fir.expressions.FirLoop
import org.jetbrains.kotlin.fir.references.toResolvedPropertySymbol
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.*
import org.jetbrains.kotlin.fir.resolve.dfa.controlFlowGraph
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol

class ControlFlowAnalysisDiagnosticComponent(
    session: FirSession,
    reporter: DiagnosticReporter,
    declarationCheckers: DeclarationCheckers,
) : AbstractDiagnosticCollectorComponent(session, reporter) {
    constructor(session: FirSession, reporter: DiagnosticReporter, mppKind: MppCheckerKind) : this(
        session,
        reporter,
        when (mppKind) {
            MppCheckerKind.Common -> session.checkersComponent.commonDeclarationCheckers
            MppCheckerKind.Platform -> session.checkersComponent.platformDeclarationCheckers
        }
    )

    private val cfaCheckers = declarationCheckers.controlFlowAnalyserCheckers
    private val variableAssignmentCheckers = declarationCheckers.variableAssignmentCfaBasedCheckers

    private fun analyze(declaration: FirControlFlowGraphOwner, context: CheckerContext) {
        val graph = declaration.controlFlowGraphReference?.controlFlowGraph ?: return
        if (graph.isSubGraph) return
        cfaCheckers.forEach { it.analyze(graph, reporter, context) }

        val collector = LocalPropertyCollector().apply { graph.traverse(this) }
        val properties = collector.properties
        if (properties.isNotEmpty()) {
            val data = PropertyInitializationInfoData(properties, collector.conditionallyInitializedProperties, receiver = null, graph)
            variableAssignmentCheckers.forEach { it.analyze(data, reporter, context) }
        }
    }

    // ------------------------------- File -------------------------------

    override fun visitFile(file: FirFile, data: CheckerContext) {
        analyze(file, data)
    }

    // ------------------------------- File -------------------------------

    override fun visitScript(script: FirScript, data: CheckerContext) {
        analyze(script, data)
    }

    // ------------------------------- Class initializer -------------------------------

    override fun visitRegularClass(regularClass: FirRegularClass, data: CheckerContext) {
        analyze(regularClass, data)
    }

    override fun visitAnonymousObject(anonymousObject: FirAnonymousObject, data: CheckerContext) {
        analyze(anonymousObject, data)
    }

    // ------------------------------- Property initializer -------------------------------

    override fun visitProperty(property: FirProperty, data: CheckerContext) {
        analyze(property, data)
    }

    // ------------------------------- Function -------------------------------

    override fun visitFunction(function: FirFunction, data: CheckerContext) {
        analyze(function, data)
    }

    override fun visitSimpleFunction(simpleFunction: FirSimpleFunction, data: CheckerContext) {
        analyze(simpleFunction, data)
    }

    override fun visitPropertyAccessor(propertyAccessor: FirPropertyAccessor, data: CheckerContext) {
        analyze(propertyAccessor, data)
    }

    override fun visitConstructor(constructor: FirConstructor, data: CheckerContext) {
        analyze(constructor, data)
    }

    private class LocalPropertyCollector : ControlFlowGraphVisitorVoid() {
        val properties = mutableSetOf<FirPropertySymbol>()

        // Properties which may not be initialized when accessed, even if they have an initializer.
        val conditionallyInitializedProperties = mutableSetOf<FirPropertySymbol>()

        // Properties defined within do-while loops, and used within the condition of that same do-while loop, are considered conditionally
        // initialized. It is possible they may not even be defined by the loop condition due to a `continue` in the do-while loop. Track
        // do-while loop properties so those used in the condition can be recorded.
        private val doWhileLoopProperties = ArrayDeque<Pair<FirLoop, MutableSet<FirPropertySymbol>>>()
        private val insideDoWhileConditions = mutableSetOf<FirLoop>()

        override fun visitNode(node: CFGNode<*>) {}

        override fun visitVariableDeclarationNode(node: VariableDeclarationNode) {
            val symbol = node.fir.symbol
            properties.add(symbol)
            doWhileLoopProperties.lastOrNull()?.second?.add(symbol)
        }

        override fun visitQualifiedAccessNode(node: QualifiedAccessNode) {
            if (insideDoWhileConditions.isNotEmpty()) {
                val symbol = node.fir.calleeReference.toResolvedPropertySymbol() ?: return

                // It is possible to nest do-while loops within do-while loop conditions via in-place lambda functions. Make sure to check
                // all properties for all loop conditions.
                if (doWhileLoopProperties.any { it.first in insideDoWhileConditions && symbol in it.second }) {
                    conditionallyInitializedProperties.add(symbol)
                }
            }
        }

        override fun visitLoopEnterNode(node: LoopEnterNode) {
            if (node.fir is FirDoWhileLoop) {
                doWhileLoopProperties.addLast(node.fir to mutableSetOf())
            }
        }

        override fun visitLoopExitNode(node: LoopExitNode) {
            if (node.fir is FirDoWhileLoop) {
                doWhileLoopProperties.removeLast()
            }
        }

        override fun visitLoopConditionEnterNode(node: LoopConditionEnterNode) {
            if (node.loop is FirDoWhileLoop) {
                insideDoWhileConditions.add(node.loop)
            }
        }

        override fun visitLoopConditionExitNode(node: LoopConditionExitNode) {
            if (node.loop is FirDoWhileLoop) {
                insideDoWhileConditions.remove(node.loop)
            }
        }
    }
}
