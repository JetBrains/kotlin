/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.cfa.util

import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.mutate
import kotlinx.collections.immutable.persistentMapOf
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.*

typealias PathAwareControlFlowInfo<I> = PersistentMap<EdgeLabel, I>

fun <I : ControlFlowInfo<I, *, *>> PathAwareControlFlowInfo<I>.join(
    other: PathAwareControlFlowInfo<I>,
    union: Boolean
): PathAwareControlFlowInfo<I> = mutate {
    for ((label, rightValue) in other) {
        // disjoint merging to preserve paths. i.e., merge the property initialization info if and only if both have the key.
        // merge({ |-> I1 }, { |-> I2, l1 |-> I3 })
        //   == { |-> merge(I1, I2), l1 |-> I3 }
        it[label] = this[label]?.let { leftValue ->
            if (union) leftValue.plus(rightValue) else leftValue.merge(rightValue)
        } ?: rightValue
    }
}

abstract class PathAwareControlFlowGraphVisitor<I : ControlFlowInfo<I, *, *>> :
    ControlFlowGraphVisitor<PathAwareControlFlowInfo<I>, PathAwareControlFlowInfo<I>>() {

    abstract val emptyInfo: PathAwareControlFlowInfo<I>

    open fun visitEdge(from: CFGNode<*>, to: CFGNode<*>, metadata: Edge, data: PathAwareControlFlowInfo<I>): PathAwareControlFlowInfo<I> {
        val label = metadata.label
        if (label.isNormal) {
            // Special case: when we exit the try expression, null label means a normal path.
            // Filter out any info bound to non-null label
            // One day, if we allow multiple edges between nodes with different labels, e.g., labeling all paths in try/catch/finally,
            // instead of this kind of special handling, proxy enter/exit nodes per label are preferred.
            if (to is TryExpressionExitNode) {
                val infoAtNormalPath = data[NormalPath] ?: return emptyInfo
                return persistentMapOf(NormalPath to infoAtNormalPath)
            }
            // In general, null label means no additional path info, hence return `this` as-is.
            return data
        }
        return if (data.keys.any { !it.isNormal }) {
            // { |-> ..., l1 |-> I1, l2 |-> I2, ... }
            //   | l1         // path exit: if the given info has non-null labels, this acts like a filtering
            // { |-> I1 }     // NB: remove the path label, except for uncaught exception path
            val info = data[label] ?: return emptyInfo
            if (label == UncaughtExceptionPath) {
                // Special case: uncaught exception path, which still represents an uncaught exception path
                // Target node is most likely fun/init exit, and we should keep info separated.
                persistentMapOf(label to info)
            } else {
                // { |-> I }
                //   | l1       // e.g., enter to proxy1 with l1
                // { l1 -> I }
                //   ...
                // { |-> ..., l1 -> I', ... }
                //   | l1       // e.g., exit proxy1 with l1
                // { l1 -> I' }
                persistentMapOf(NormalPath to info)
            }
        } else {
            // { |-> ... }    // empty path info
            //   | l1         // path entry
            // { l1 -> ... }  // now, every info bound to the label
            persistentMapOf(label to data.infoAtNormalPath)
        }
    }

    override fun visitNode(
        node: CFGNode<*>,
        data: PathAwareControlFlowInfo<I>
    ): PathAwareControlFlowInfo<I> = data

    override fun <T> visitUnionNode(
        node: T,
        data: PathAwareControlFlowInfo<I>
    ): PathAwareControlFlowInfo<I> where T : CFGNode<*>, T : UnionNodeMarker = data
}

internal val <I> PathAwareControlFlowInfo<I>.infoAtNormalPath: I
    get() = getValue(NormalPath)
