/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.cfa

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkersComponent
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ControlFlowGraph

class FirControlFlowAnalyzer(session: FirSession) {
    private val controlFlowAnalyserCheckers = session.checkersComponent.declarationCheckers.controlFlowAnalyserCheckers
    private val variableAssignmentCheckers = session.checkersComponent.declarationCheckers.variableAssignmentCfaBasedCheckers

    fun analyzeClassInitializer(klass: FirClass<*>, graph: ControlFlowGraph, context: CheckerContext, reporter: DiagnosticReporter) {
        if (graph.owner != null) return
        // TODO()
    }

    fun analyzeFunction(function: FirFunction<*>, graph: ControlFlowGraph, context: CheckerContext, reporter: DiagnosticReporter) {
        if (graph.owner != null) return

        val properties = AbstractFirPropertyInitializationChecker.LocalPropertyCollector.collect(graph)
        if (properties.isEmpty()) return
        val data = AbstractFirPropertyInitializationChecker.DataCollector(properties).getData(graph)

        variableAssignmentCheckers.forEach { it.analyze(graph, reporter, data, properties) }
    }

    fun analyzePropertyInitializer(property: FirProperty, graph: ControlFlowGraph, context: CheckerContext, reporter: DiagnosticReporter) {
        if (graph.owner != null) return
        // TODO()
    }
}
