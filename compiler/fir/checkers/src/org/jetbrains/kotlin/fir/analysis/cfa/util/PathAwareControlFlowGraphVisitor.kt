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

    open fun visitSubGraph(node: CFGNodeWithSubgraphs<*>, graph: ControlFlowGraph): Boolean =
        true // false to skip

    open fun visitEdge(from: CFGNode<*>, to: CFGNode<*>, metadata: Edge, data: PathAwareControlFlowInfo<I>): PathAwareControlFlowInfo<I> {
        val label = metadata.label
        return when {
            // Finally exit is splitting labeled flow. So if we have data for different labels, then
            // data for each only goes along an edge with the same label, and the leftover data
            // is forwarded along an UncaughtExceptionPath edge, if any, to the next finally block.
            from is FinallyBlockExitNode -> {
                if (label == UncaughtExceptionPath) {
                    data.mutate {
                        for (other in from.followingNodes) {
                            val otherLabel = from.edgeTo(other).label
                            if (otherLabel != UncaughtExceptionPath) {
                                it.remove(otherLabel)
                            }
                        }
                    }.ifEmpty { emptyInfo } // there should always be UncaughtExceptionPath data, but just in case
                } else {
                    val info = data[label] ?: return emptyInfo
                    persistentMapOf(NormalPath to info)
                }
            }
            // A normal or postponed path forwards all data. (Non-normal paths should only have data in finally blocks.)
            label == NormalPath -> data
            label == PostponedPath -> data
            // Labeled edge from a jump statement to a `finally` block forks flow. Usually we'd only have
            // NormalPath data here, but technically it's possible (though questionable) to jump from a `finally`
            // (discarding the exception or aborting a previous jump in the process) so merge all data just in case.
            else -> persistentMapOf(label to data.values.reduce { a, b -> a.merge(b) })
        }
    }

    override fun visitNode(
        node: CFGNode<*>,
        data: PathAwareControlFlowInfo<I>
    ): PathAwareControlFlowInfo<I> = data
}
