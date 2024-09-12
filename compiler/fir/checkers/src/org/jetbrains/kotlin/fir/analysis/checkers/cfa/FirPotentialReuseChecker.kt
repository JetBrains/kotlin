/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.cfa

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ControlFlowGraph

object FirPotentialReuseChecker: FirControlFlowChecker(MppCheckerKind.Common) {
    override fun analyze(graph: ControlFlowGraph, reporter: DiagnosticReporter, context: CheckerContext) {
        val node = graph.exitNode
        val fir = node.fir
        reporter.reportOn(fir.source, FirErrors.POTENTIAL_REUSE, context)
    }
}