/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.collectors.components

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.cfa.util.PropertyInitializationInfoData
import org.jetbrains.kotlin.fir.analysis.cfa.util.VariableInitializationInfoData
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.DeclarationCheckers
import org.jetbrains.kotlin.fir.analysis.checkersComponent
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.FirDoWhileLoop
import org.jetbrains.kotlin.fir.expressions.FirLoop
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.references.toResolvedPropertySymbol
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ControlFlowGraph
import org.jetbrains.kotlin.fir.resolve.dfa.controlFlowGraph
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.visitors.FirDefaultVisitor

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

        val collector = LocalPropertyCollector().apply { declaration.acceptChildren(this, graph.subGraphs.toSet()) }
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

    /**
     * Attempts to traverse an [FirElement] like a [ControlFlowGraph]. The data of this visitor represents the allowed
     * [ControlFlowGraph.subGraphs] which can be entered. This allows limiting the visited elements to those which are part of the original
     * [ControlFlowGraph].
     *
     * ```kotlin
     * val element: FirControlFlowGraphOwner = ...
     * val graph = element.controlFlowGraphReference?.controlFlowGraph ?: ...
     * LocalPropertyCollector().apply { element.acceptChildren(this, graph.subGraphs.toSet()) }
     * ```
     */
    private class LocalPropertyCollector : FirDefaultVisitor<Unit, Set<ControlFlowGraph>>() {
        val properties = mutableSetOf<FirPropertySymbol>()

        // Properties which may not be initialized when accessed, even if they have an initializer.
        val conditionallyInitializedProperties = mutableSetOf<FirPropertySymbol>()

        // Properties defined within do-while loops, and used within the condition of that same do-while loop, are considered conditionally
        // initialized. It is possible they may not even be defined by the loop condition due to a `continue` in the do-while loop. Track
        // do-while loop properties so those used in the condition can be recorded.
        private val doWhileLoopProperties = ArrayDeque<Pair<FirLoop, MutableSet<FirPropertySymbol>>>()
        private val insideDoWhileConditions = mutableSetOf<FirLoop>()

        override fun visitElement(element: FirElement, data: Set<ControlFlowGraph>) {
            when (element) {
                is FirControlFlowGraphOwner -> {
                    // Only traverse elements that can have a graph when...
                    // 1. They do not have a graph,
                    // 2. Or their graph is in the allowed set of sub-graphs.
                    val elementGraph = element.controlFlowGraphReference?.controlFlowGraph
                    if (elementGraph == null) {
                        element.acceptChildren(this, data)
                    } else if (elementGraph in data) {
                        element.acceptChildren(this, elementGraph.subGraphs.toSet())
                    }
                }
                else -> element.acceptChildren(this, data)
            }
        }

        override fun visitProperty(property: FirProperty, data: Set<ControlFlowGraph>) {
            if (
                !property.isLocal ||
                property.origin == FirDeclarationOrigin.ScriptCustomization.Parameter ||
                property.origin == FirDeclarationOrigin.ScriptCustomization.ParameterFromBaseClass
            ) return visitElement(property, data)

            val symbol = property.symbol
            properties.add(symbol)
            doWhileLoopProperties.lastOrNull()?.second?.add(symbol)

            visitElement(property, data)
        }

        override fun visitQualifiedAccessExpression(qualifiedAccessExpression: FirQualifiedAccessExpression, data: Set<ControlFlowGraph>) {
            if (insideDoWhileConditions.isNotEmpty()) {
                val symbol = qualifiedAccessExpression.calleeReference.toResolvedPropertySymbol() ?: return

                // It is possible to nest do-while loops within do-while loop conditions via in-place lambda functions. Make sure to check
                // all properties for all loop conditions.
                if (doWhileLoopProperties.any { it.first in insideDoWhileConditions && symbol in it.second }) {
                    conditionallyInitializedProperties.add(symbol)
                }
            }

            visitElement(qualifiedAccessExpression, data)
        }

        override fun visitDoWhileLoop(doWhileLoop: FirDoWhileLoop, data: Set<ControlFlowGraph>) {
            doWhileLoopProperties.addLast(doWhileLoop to mutableSetOf())

            // Manually navigate children of do-while loop, so it is known when the loop condition is being navigated.
            // Navigation of the annotations and label is not needed.
            doWhileLoop.block.accept(this, data)

            insideDoWhileConditions.add(doWhileLoop)
            doWhileLoop.condition.accept(this, data)
            insideDoWhileConditions.remove(doWhileLoop)

            doWhileLoopProperties.removeLast()
        }
    }
}
