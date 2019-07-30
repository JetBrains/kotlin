/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.concurrent

import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class SynchronizedComputeCache<K : Any, V>(val storage: ReadLockFreeOpenAddressingHashMap<K, Any> = ReadLockFreeOpenAddressingHashMap()) :
    AbstractLookupCache<K, V>() {

    private val lock = ReentrantLock()

    override fun lookup(key: K, compute: (K) -> V): V {
        val v = storage[key]
        return unbox(
            if (v == null) {
                assert(!lock.isHeldByCurrentThread) { "SynchronizedComputeCache doesn't support recursive calculation" }
                lock.withLock {
                    val u = storage[key]
                    if (u == null) {
                        val computed = compute(key) ?: NULL
                        storage[key] = computed
                        computed
                    } else {
                        u
                    }
                }
            } else v
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun unbox(v: Any): V = (if (v === NULL) null else v) as V
}

private val NULL = Any()