/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.collectors.components

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.DeclarationCheckers
import org.jetbrains.kotlin.fir.analysis.checkersComponent
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.cfa.util.LocalPropertyAndCapturedWriteCollector
import org.jetbrains.kotlin.fir.analysis.cfa.util.PropertyInitializationInfoData
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.resolve.dfa.controlFlowGraph

class ControlFlowAnalysisDiagnosticComponent(
    session: FirSession,
    reporter: DiagnosticReporter,
    declarationCheckers: DeclarationCheckers = session.checkersComponent.declarationCheckers,
) : AbstractDiagnosticCollectorComponent(session, reporter) {
    private val cfaCheckers = declarationCheckers.controlFlowAnalyserCheckers
    private val variableAssignmentCheckers = declarationCheckers.variableAssignmentCfaBasedCheckers

    private fun analyze(declaration: FirControlFlowGraphOwner, context: CheckerContext) {
        val graph = declaration.controlFlowGraphReference?.controlFlowGraph ?: return
        if (graph.owner != null) return
        cfaCheckers.forEach { it.analyze(graph, reporter, context) }

        val (properties, capturedWrites) = LocalPropertyAndCapturedWriteCollector.collect(graph)
        if (properties.isNotEmpty()) {
            val data = PropertyInitializationInfoData(properties, graph)
            variableAssignmentCheckers.forEach { it.analyze(graph, reporter, data, properties, capturedWrites, context) }
        }
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
}
