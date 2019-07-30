/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.concurrent

import java.lang.RuntimeException
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class NonRecursiveCooperativeComputeCache<K : Any, V> : AbstractLookupCache<K, V>() {

    val storage = ReadLockFreeOpenAddressingHashMap<K, Any>()

    val index = AtomicInteger()

    @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
    class Computation(val index: Int) {
        var completed: Boolean = false

        @Synchronized
        fun await() {
            while (!completed) (this as Object).wait()
        }

        @Synchronized
        fun complete() {
            completed = true
            (this as Object).notifyAll()
        }
    }

    val lock = ReentrantLock()


    private fun Computation.beginCompute(key: K): Any? {
        while (true) {
            val otherComputation: Computation = lock.withLock {
                val nv = storage[key]
                when (nv) {
                    is Computation -> {
                        if (!nv.completed && nv.index < this.index) {
                            return@withLock nv
                        }
                        storage[key] = this
                        return null
                    }
                    null -> {
                        storage[key] = this
                        return nv
                    }
                    else -> return nv
                }
            }
            otherComputation.await()
        }
    }

    private fun Computation.commit(key: K, value: Any): Boolean {
        return try {
            lock.withLock {
                if (storage[key] !is Computation) return@withLock false
                storage[key] = value
                true
            }
        } finally {
            complete()
        }
    }

    override tailrec fun lookup(key: K, compute: (K) -> V): V {
        val v = storage[key]

        if (v != null && v !is Computation) return unbox(v)

        val computation = Computation(index.getAndIncrement())
        val nv = computation.beginCompute(key)
        val computed: Any
        if (nv == null) {
            try {
                computed = compute(key) ?: NULL
            } catch (t: Throwable) {
                computation.complete()
                throw RuntimeException("Exception in computation", t)
            }
            if (!computation.commit(key, computed)) {
                return lookup(key, compute)
            }
        } else {
            computed = nv
        }
        return unbox(computed)

    }

    @Suppress("UNCHECKED_CAST")
    private fun unbox(v: Any): V = (if (v === NULL) null else v) as V
}

private val NULL = Any()