/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.cfa.util

import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.mutate
import kotlinx.collections.immutable.persistentMapOf
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.*

typealias ControlFlowInfo<K, V> = PersistentMap<K, V>

typealias PathAwareControlFlowInfo<K, V> = PersistentMap<EdgeLabel, ControlFlowInfo<K, V>>

// The inputs to `PathAwareControlFlowGraphVisitor`'s methods should contain keys for all labels with
// which a node can be reached, so a completely empty `PathAwareControlFlowInfo` is NOT equivalent
// to one that contains empty data for just the `NormalPath`.
private val EMPTY_NORMAL_PATH_INFO: PathAwareControlFlowInfo<Nothing, Nothing> =
    persistentMapOf(NormalPath to persistentMapOf())

@Suppress("UNCHECKED_CAST")
fun <K : Any, V : Any> emptyNormalPathInfo(): PathAwareControlFlowInfo<K, V> =
    EMPTY_NORMAL_PATH_INFO as PathAwareControlFlowInfo<K, V>

abstract class PathAwareControlFlowGraphVisitor<K : Any, V : Any> :
    ControlFlowGraphVisitor<PathAwareControlFlowInfo<K, V>, PathAwareControlFlowInfo<K, V>>() {

    abstract fun mergeInfo(a: ControlFlowInfo<K, V>, b: ControlFlowInfo<K, V>, node: CFGNode<*>): ControlFlowInfo<K, V>

    @JvmName("mergePathAwareInfo")
    fun mergeInfo(a: PathAwareControlFlowInfo<K, V>, b: PathAwareControlFlowInfo<K, V>, node: CFGNode<*>): PathAwareControlFlowInfo<K, V> =
        a.merge(b) { left, right -> mergeInfo(left, right, node) }

    open fun visitSubGraph(node: CFGNodeWithSubgraphs<*>, graph: ControlFlowGraph): Boolean =
        true // false to skip

    open fun visitEdge(
        from: CFGNode<*>,
        to: CFGNode<*>,
        metadata: Edge,
        data: PathAwareControlFlowInfo<K, V>,
    ): PathAwareControlFlowInfo<K, V> {
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
                    }
                } else {
                    val info = data[label] ?: return emptyNormalPathInfo()
                    persistentMapOf(NormalPath to info)
                }
            }
            // A normal or postponed path forwards all data. (Non-normal paths should only have data in finally blocks.)
            label == NormalPath -> data
            label == PostponedPath -> data
            // Labeled edge from a jump statement to a `finally` block forks flow. Usually we'd only have
            // NormalPath data here, but technically it's possible (though questionable) to jump from a `finally`
            // (discarding the exception or aborting a previous jump in the process) so merge all data just in case.
            else -> persistentMapOf(label to data.values.reduce { a, b -> mergeInfo(a, b, to) })
        }
    }

    override fun visitNode(
        node: CFGNode<*>,
        data: PathAwareControlFlowInfo<K, V>
    ): PathAwareControlFlowInfo<K, V> = data
}

inline fun <K, V> PersistentMap<K, V>.merge(other: PersistentMap<K, V>, block: (V, V) -> V): PersistentMap<K, V> =
    mutate {
        other.mapValuesTo(it) { (label, rightValue) ->
            this[label]?.let { leftValue -> block(leftValue, rightValue) } ?: rightValue
        }
    }

inline fun <K, V> PersistentMap<K, V>.transformValues(block: (V) -> V): PersistentMap<K, V> =
    mutate { mapValuesTo(it) { (_, values) -> block(values) } }
