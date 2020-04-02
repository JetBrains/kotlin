/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.collectors.components

import org.jetbrains.kotlin.fir.analysis.cfa.FirControlFlowAnalyzer
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.collectors.AbstractDiagnosticCollector
import org.jetbrains.kotlin.fir.references.FirControlFlowGraphReference
import org.jetbrains.kotlin.fir.resolve.dfa.controlFlowGraph

class ControlFlowAnalysisDiagnosticComponent(collector: AbstractDiagnosticCollector) : AbstractDiagnosticCollectorComponent(collector) {
    override fun visitControlFlowGraphReference(controlFlowGraphReference: FirControlFlowGraphReference, data: CheckerContext) {
        val graph = controlFlowGraphReference.controlFlowGraph ?: return
        runCheck {
            val controlFlowAnalyzer = FirControlFlowAnalyzer(it)
            controlFlowAnalyzer.analyze(graph, data)
        }
    }
}