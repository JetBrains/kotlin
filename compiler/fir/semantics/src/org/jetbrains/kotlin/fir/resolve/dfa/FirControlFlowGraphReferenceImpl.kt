/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dfa

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.references.FirControlFlowGraphReference
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.CFGNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ControlFlowGraph
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.jvm.specialization.annotations.Monomorphic

class FirControlFlowGraphReferenceImpl(
    val controlFlowGraph: ControlFlowGraph,
    val dataFlowInfo: DataFlowInfo? = null
) : FirControlFlowGraphReference() {
    override val source: KtSourceElement? get() = null

    override fun <R, D, @Monomorphic VT : FirVisitor<R, D>> acceptChildren(visitor: VT, data: D) {}

    override fun <D, @Monomorphic TT: FirTransformer<D>> transformChildren(transformer: TT, data: D): FirControlFlowGraphReference {
        return this
    }
}

class DataFlowInfo(val variableStorage: VariableStorage)

val FirControlFlowGraphReference.controlFlowGraph: ControlFlowGraph?
    get() = (this as? FirControlFlowGraphReferenceImpl)?.controlFlowGraph

val FirControlFlowGraphReference.dataFlowInfo: DataFlowInfo?
    get() = (this as? FirControlFlowGraphReferenceImpl)?.dataFlowInfo
