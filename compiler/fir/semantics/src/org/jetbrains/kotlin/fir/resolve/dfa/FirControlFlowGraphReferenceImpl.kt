/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dfa

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.references.FirControlFlowGraphReference
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.CFGNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ControlFlowGraph
import org.jetbrains.kotlin.fir.visitors.FirElementKind
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitor

class FirControlFlowGraphReferenceImpl(
    val controlFlowGraph: ControlFlowGraph,
    val dataFlowInfo: DataFlowInfo? = null
) : FirControlFlowGraphReference() {
    override val source: KtSourceElement? get() = null

    override val elementKind: FirElementKind
        get() = FirElementKind.ControlFlowGraphReference
}

class DataFlowInfo(val variableStorage: VariableStorage, val flowOnNodes: Map<CFGNode<*>, Flow>)

val FirControlFlowGraphReference.controlFlowGraph: ControlFlowGraph?
    get() = (this as? FirControlFlowGraphReferenceImpl)?.controlFlowGraph

val FirControlFlowGraphReference.dataFlowInfo: DataFlowInfo?
    get() = (this as? FirControlFlowGraphReferenceImpl)?.dataFlowInfo
