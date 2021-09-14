/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.cfa.util

import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.*

abstract class PathAwareControlFlowInfo<P : PathAwareControlFlowInfo<P, S>, S : ControlFlowInfo<S, *, *>>(
    map: PersistentMap<EdgeLabel, S>,
) : ControlFlowInfo<P, EdgeLabel, S>(map) {

    internal val infoAtNormalPath: S
        get() = map.getValue(NormalPath)

    private val hasNormalPath: Boolean
        get() = map.containsKey(NormalPath)

    fun applyLabel(node: CFGNode<*>, label: EdgeLabel): P {
        if (label.isNormal) {
            // Special case: when we exit the try expression, null label means a normal path.
            // Filter out any info bound to non-null label
            // One day, if we allow multiple edges between nodes with different labels, e.g., labeling all paths in try/catch/finally,
            // instead of this kind of special handling, proxy enter/exit nodes per label are preferred.
            if (node is TryExpressionExitNode) {
                return if (hasNormalPath) {
                    constructor(persistentMapOf(NormalPath to infoAtNormalPath))
                } else {
                    /* This means no info for normal path. */
                    empty()
                }
            }
            // In general, null label means no additional path info, hence return `this` as-is.
            @Suppress("UNCHECKED_CAST")
            return this as P
        }

        val hasAbnormalLabels = map.keys.any { !it.isNormal }
        return if (hasAbnormalLabels) {
            // { |-> ..., l1 |-> I1, l2 |-> I2, ... }
            //   | l1         // path exit: if the given info has non-null labels, this acts like a filtering
            // { |-> I1 }     // NB: remove the path label, except for uncaught exception path
            if (map.keys.contains(label)) {
                if (label == UncaughtExceptionPath) {
                    // Special case: uncaught exception path, which still represents an uncaught exception path
                    // Target node is most likely fun/init exit, and we should keep info separated.
                    constructor(persistentMapOf(label to map[label]!!))
                } else {
                    // { |-> I }
                    //   | l1       // e.g., enter to proxy1 with l1
                    // { l1 -> I }
                    //   ...
                    // { |-> ..., l1 -> I', ... }
                    //   | l1       // e.g., exit proxy1 with l1
                    // { l1 -> I' }
                    constructor(persistentMapOf(NormalPath to map[label]!!))
                }
            } else {
                /* This means no info for the specific label. */
                empty()
            }
        } else {
            // { |-> ... }    // empty path info
            //   | l1         // path entry
            // { l1 -> ... }  // now, every info bound to the label
            constructor(persistentMapOf(label to infoAtNormalPath))
        }
    }

    override fun merge(other: P): P {
        var resultMap = persistentMapOf<EdgeLabel, S>()
        for (label in keys.union(other.keys)) {
            // disjoint merging to preserve paths. i.e., merge the property initialization info if and only if both have the key.
            // merge({ |-> I1 }, { |-> I2, l1 |-> I3 })
            //   == { |-> merge(I1, I2), l1 |-> I3 }
            val i1 = this[label]
            val i2 = other[label]
            resultMap = when {
                i1 != null && i2 != null ->
                    resultMap.put(label, i1.merge(i2))
                i1 != null ->
                    resultMap.put(label, i1)
                i2 != null ->
                    resultMap.put(label, i2)
                else ->
                    throw IllegalStateException()
            }
        }
        return constructor(resultMap)
    }
}
