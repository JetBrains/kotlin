/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.cfa

import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.CFGNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ControlFlowGraph
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol

abstract class AbstractFirPropertyInitializationChecker {
    abstract fun analyze(
        graph: ControlFlowGraph,
        reporter: DiagnosticReporter,
        data: Map<CFGNode<*>, PathAwarePropertyInitializationInfo>,
        properties: Set<FirPropertySymbol>
    )
}