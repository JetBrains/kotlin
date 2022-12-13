/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.cfa.util

import org.jetbrains.kotlin.fir.resolve.dfa.cfg.CFGNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ControlFlowGraphVisitor
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.Edge
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.UnionNodeMarker

abstract class PathAwareControlFlowGraphVisitor<P : PathAwareControlFlowInfo<P, *>> : ControlFlowGraphVisitor<P, P>() {
    abstract val emptyInfo: P

    open fun visitEdge(from: CFGNode<*>, to: CFGNode<*>, metadata: Edge, data: P): P =
        data.applyLabel(to, metadata.label) ?: emptyInfo

    override fun visitNode(node: CFGNode<*>, data: P): P = data

    override fun <T> visitUnionNode(node: T, data: P): P where T : CFGNode<*>, T : UnionNodeMarker = data
}
