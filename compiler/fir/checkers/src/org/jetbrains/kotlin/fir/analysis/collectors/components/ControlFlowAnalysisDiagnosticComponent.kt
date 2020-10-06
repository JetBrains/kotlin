/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.collectors.components

import org.jetbrains.kotlin.fir.analysis.cfa.FirControlFlowAnalyzer
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.collectors.AbstractDiagnosticCollector
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.references.FirControlFlowGraphReference
import org.jetbrains.kotlin.fir.resolve.dfa.controlFlowGraph

class ControlFlowAnalysisDiagnosticComponent(collector: AbstractDiagnosticCollector) : AbstractDiagnosticCollectorComponent(collector) {
    private val controlFlowAnalyzer = FirControlFlowAnalyzer(session)

    // ------------------------------- Class initializer -------------------------------

    override fun visitRegularClass(regularClass: FirRegularClass, data: CheckerContext) {
        visitClass(regularClass, regularClass.controlFlowGraphReference, data)
    }

    override fun visitAnonymousObject(anonymousObject: FirAnonymousObject, data: CheckerContext) {
        visitClass(anonymousObject, anonymousObject.controlFlowGraphReference, data)
    }

    private fun <F : FirClass<F>> visitClass(
        klass: FirClass<F>,
        controlFlowGraphReference: FirControlFlowGraphReference?,
        data: CheckerContext
    ) {
        val graph = controlFlowGraphReference?.controlFlowGraph ?: return
        controlFlowAnalyzer.analyzeClassInitializer(klass, graph, data, reporter)
    }

    // ------------------------------- Property initializer -------------------------------
    override fun visitProperty(property: FirProperty, data: CheckerContext) {
        val graph = property.controlFlowGraphReference?.controlFlowGraph ?: return
        controlFlowAnalyzer.analyzePropertyInitializer(property, graph, data, reporter)
    }

    // ------------------------------- Function -------------------------------

    override fun <F : FirFunction<F>> visitFunction(function: FirFunction<F>, data: CheckerContext) {
        val graph = function.controlFlowGraphReference?.controlFlowGraph ?: return

        controlFlowAnalyzer.analyzeFunction(function, graph, data, reporter)
    }

    override fun visitSimpleFunction(simpleFunction: FirSimpleFunction, data: CheckerContext) {
        visitFunction(simpleFunction, data)
    }

    override fun visitPropertyAccessor(propertyAccessor: FirPropertyAccessor, data: CheckerContext) {
        val graph = propertyAccessor.controlFlowGraphReference?.controlFlowGraph ?: return

        controlFlowAnalyzer.analyzePropertyAccessor(propertyAccessor, graph, data, reporter)
    }

    override fun visitConstructor(constructor: FirConstructor, data: CheckerContext) {
        visitFunction(constructor, data)
    }
}
