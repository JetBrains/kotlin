/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dfa

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.references.FirControlFlowGraphNodeReference
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.CFGNode
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitor

class FirControlFlowGraphNodeReferenceImpl(
    val controlFlowGraphNode: CFGNode<*>,
) : FirControlFlowGraphNodeReference() {
    override val source: KtSourceElement? get() = null

    override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {}

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirControlFlowGraphNodeReference {
        return this
    }
}

val FirControlFlowGraphNodeReference.controlFlowGraphNode: CFGNode<*>?
    get() = (this as? FirControlFlowGraphNodeReferenceImpl)?.controlFlowGraphNode
