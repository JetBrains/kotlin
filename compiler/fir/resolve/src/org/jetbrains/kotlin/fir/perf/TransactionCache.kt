/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.perf

import java.lang.RuntimeException
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.getOrSet
import kotlin.concurrent.withLock

val tcReg = mutableListOf<TransactionCache<*, *>>()

class TransactionCache<K : Any, V>(val storage: ReadLockFreeOpenAddressingHashMap<K, Any> = ReadLockFreeOpenAddressingHashMap()) {
    init {
        tcReg += this
    }

    private var commits = 0L
    private var retries = 0L
    private var joins = 0L

    fun stats(): String {
        return "Commits: $commits, Success: ${commits - retries}, Retries: $retries, Joins: $joins"
    }

    private val transaction = ThreadLocal<Transaction<K>>()

    class Transaction<K>(
        val index: Int,
        val transactionStorage: MutableMap<K, Any>
    ) {
        var completed = false
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

    private val indexCounter = AtomicInteger()

    private val lock = ReentrantLock()

    private fun beginCompute(currentTransaction: Transaction<K>, key: K): Any? {
        while (true) {
            val v: Transaction<*> = lock.withLock {
                val v = storage[key]
                when (v) {
                    is Transaction<*> -> {
                        if (v.index < currentTransaction.index && !v.completed) {
                            joins++
                            return@withLock v
                        }
                        storage[key] = currentTransaction
                        return null
                    }
                    null -> storage[key] = currentTransaction
                }
                return v
            }
            v.await()
        }
    }

    private fun Transaction<K>.commit(): Boolean {
        return lock.withLock {
            commits++
            val canCommit = transactionStorage.keys.all {
                val v = storage[it]
                v is Transaction<*>?
            }
            this.complete()
            transaction.remove()

            if (!canCommit) {
                retries++
                return@withLock false
            }


            storage.putAll(transactionStorage)
            true
        }
    }

    private fun Transaction<K>.abort() {
        this.complete()
        transaction.remove()
    }

    fun storeToTransaction(key: K, value: V) {
        transaction.get().transactionStorage[key] = value ?: NULL
    }

    fun readFromTransaction(key: K): V {
        return unbox(transaction.get().transactionStorage[key]!!)
    }

    tailrec fun lookup(key: K, compute: (K) -> V, postCompute: (V) -> Unit): V? {

        val v = storage[key]
        if (v != null && v !is Transaction<*>) return unbox(v)


        var startedTransaction = false
        val currentTransaction = transaction.getOrSet {
            startedTransaction = true
            Transaction(indexCounter.getAndIncrement(), mutableMapOf())
        }

        val computed: Any
        val cached = currentTransaction.transactionStorage[key]
        if (cached != null)
            return unbox(cached)
        else {
            val v = beginCompute(currentTransaction, key)
            if (v == null) {
                try {
                    computed = compute(key) as Any? ?: NULL
                    currentTransaction.transactionStorage[key] = computed
                    postCompute(unbox(computed))
                } catch (t: Throwable) {
                    currentTransaction.abort()
                    t.printStackTrace()
                    throw RuntimeException("Exception in compute", t)
                }
            } else {
                computed = v
            }
        }
        if (startedTransaction) {
            if (!currentTransaction.commit()) {
                return lookup(key, compute, postCompute)
            }
        }
        return unbox(computed)
    }

    private fun unbox(computed: Any) = (if (computed === NULL) null else computed) as V


}

private val NULL = Any()



