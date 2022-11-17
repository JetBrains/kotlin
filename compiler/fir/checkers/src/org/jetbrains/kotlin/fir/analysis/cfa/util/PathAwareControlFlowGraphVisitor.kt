/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.cfa.util

import org.jetbrains.kotlin.fir.resolve.dfa.cfg.CFGNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ControlFlowGraphVisitor
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.EdgeLabel
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.UnionNodeMarker
import org.jetbrains.kotlin.utils.addToStdlib.foldMap

abstract class PathAwareControlFlowGraphVisitor<P : PathAwareControlFlowInfo<P, *>>
    : ControlFlowGraphVisitor<P, Collection<Pair<EdgeLabel, P>>>() {

    protected abstract val emptyInfo: P

    override fun visitNode(node: CFGNode<*>, data: Collection<Pair<EdgeLabel, P>>): P {
        if (data.isEmpty()) return emptyInfo
        return data.foldMap({ (label, info) -> info.applyLabel(node, label) }) { a, b -> a.merge(b) }
    }

    override fun <T> visitUnionNode(node: T, data: Collection<Pair<EdgeLabel, P>>): P where T : CFGNode<*>, T : UnionNodeMarker {
        if (data.isEmpty()) return emptyInfo
        return data.foldMap({ (label, info) -> info.applyLabel(node, label) }) { a, b -> a.plus(b) }
    }
}
