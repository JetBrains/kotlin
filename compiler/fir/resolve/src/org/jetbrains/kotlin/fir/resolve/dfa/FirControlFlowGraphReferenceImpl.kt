/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dfa

import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.references.FirControlFlowGraphReference
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.CFGNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ControlFlowGraph
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitor

class FirControlFlowGraphReferenceImpl(
    val controlFlowGraph: ControlFlowGraph,
    val dataFlowInfo: DataFlowInfo? = null
) : FirControlFlowGraphReference() {
    override val source: FirSourceElement? get() = null

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {}

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirControlFlowGraphReference {
        return this
    }
}

class DataFlowInfo(val variableStorage: VariableStorage, val flowOnNodes: Map<CFGNode<*>, Flow>)

val FirControlFlowGraphReference.controlFlowGraph: ControlFlowGraph?
    get() = (this as? FirControlFlowGraphReferenceImpl)?.controlFlowGraph

val FirControlFlowGraphReference.dataFlowInfo: DataFlowInfo?
    get() = (this as? FirControlFlowGraphReferenceImpl)?.dataFlowInfo