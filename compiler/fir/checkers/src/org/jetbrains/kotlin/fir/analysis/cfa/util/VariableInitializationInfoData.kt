/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.cfa.util

import org.jetbrains.kotlin.fir.resolve.dfa.cfg.CFGNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ControlFlowGraph
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol

typealias VariableInitializationEvent = FirVariableSymbol<*>
typealias VariableInitializationInfo = EventOccurrencesRangeInfo<VariableInitializationEvent>
typealias PathAwarePropertyInitializationInfo = PathAwareEventOccurrencesRangeInfo<VariableInitializationEvent>

abstract class VariableInitializationInfoData {
    abstract val properties: Set<FirVariableSymbol<*>>
    abstract val conditionallyInitializedProperties: Set<FirVariableSymbol<*>>
    abstract val receiver: FirBasedSymbol<*>?
    abstract val graph: ControlFlowGraph

    abstract fun getValue(node: CFGNode<*>): PathAwarePropertyInitializationInfo
}
