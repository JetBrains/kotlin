/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.cfa.util

import kotlinx.collections.immutable.PersistentMap

abstract class ControlFlowInfo<S : ControlFlowInfo<S, K, V>, K : Any, V : Any> protected constructor(
    protected val map: PersistentMap<K, V>,
) : PersistentMap<K, V> by map {

    protected abstract val constructor: (PersistentMap<K, V>) -> S

    override fun equals(other: Any?): Boolean {
        return map == (other as? ControlFlowInfo<*, *, *>)?.map
    }

    override fun hashCode(): Int {
        return map.hashCode()
    }

    override fun toString(): String {
        return map.toString()
    }

    override fun put(key: K, value: V): S {
        return constructor(map.put(key, value))
    }

    override fun remove(key: K): S {
        return constructor(map.remove(key))
    }

    abstract fun merge(other: S): S

    abstract fun plus(other: S): S
}
