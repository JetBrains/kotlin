/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.fir.caches

import org.jetbrains.kotlin.analysis.low.level.api.fir.util.lockWithPCECheck
import java.util.concurrent.locks.ReentrantLock

/**
 * Lazily calculated value which runs postCompute in the same thread,
 * assuming that postCompute may try to read that value inside current thread,
 * So in the period then value is calculated but post compute was not finished,
 * only thread that initiated the calculating may see the value,
 * other threads will have to wait until that value is calculated
 */
internal class ValueWithPostCompute<KEY, VALUE, DATA>(
    /**
     * We need at least one final field to be written in constructor to guarantee safe initialization of our [ValueWithPostCompute]
     */
    private val key: KEY,
    calculate: (KEY) -> Pair<VALUE, DATA>,
    postCompute: (KEY, VALUE, DATA) -> Unit,
    sharedComputationLock: ReentrantLock? = null,
) {
    private var _calculate: ((KEY) -> Pair<VALUE, DATA>)? = calculate
    private var _postCompute: ((KEY, VALUE, DATA) -> Unit)? = postCompute

    /**
     * [lock] being volatile ensures the consistent reads between [lock] and [value] in different threads.
     */
    @Volatile
    private var lock: ReentrantLock? = sharedComputationLock ?: ReentrantLock()

    /**
     * [value] can be in one of the following states:
     *
     *  - [ValueIsNotComputed] -- The value has not been initialized yet.
     *  - [ValueIsCalculatingNow] -- The value is currently being calculated with [_calculate].
     *  - [ValueIsPostComputingNow] -- The value is currently being post-computed with [_postCompute]. The thread with `threadId` has
     *  calculated the value, and only it can access the value during post-computation.
     *  - A value of type [VALUE] -- The value has been calculated and post-computed. It is visible for all threads.
     *
     * [value] may be set only under [ValueWithPostCompute] intrinsic lock hold, and may be read from any thread.
     */
    @Volatile
    private var value: Any? = ValueIsNotComputed

    @Suppress("UNCHECKED_CAST")
    fun getValue(): VALUE {
        when (val stateSnapshot = value) {
            is ValueIsPostComputingNow -> {
                if (stateSnapshot.threadId == Thread.currentThread().id) {
                    return stateSnapshot.value as VALUE
                } else {
                    lock?.lockWithPCECheck(LOCKING_INTERVAL_MS) { // wait until other thread which holds the lock now computes the value
                        when (value) {
                            ValueIsNotComputed -> {
                                // if we have a PCE during value computation, then we will enter the critical section with `value == ValueIsNotComputed`
                                // in this case, we should try to recalculate the value
                                return computeValueWithoutLock()
                            }

                            else -> {
                                // other thread computed the value for us
                                return value as VALUE
                            }
                        }
                    } ?: return value as VALUE
                }
            }

            ValueIsNotComputed, ValueIsCalculatingNow -> lock?.lockWithPCECheck(LOCKING_INTERVAL_MS) {
                return computeValueWithoutLock()
            } ?: return value as VALUE

            else -> {
                return stateSnapshot as VALUE
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    // should be called under a synchronized section
    private fun computeValueWithoutLock(): VALUE {
        // if we entered synchronized section that's mean that the value is not yet calculated and was not started to be calculated
        // or the some other thread calculated the value while we were waiting to acquire the lock

        when (value) {
            ValueIsNotComputed -> {
                // will be computed later, the read of `ValueIsNotComputed` guarantees that lock is not null
                require(lock!!.isHeldByCurrentThread)
            }
            ValueIsCalculatingNow -> {
                error("Value calculation should not access the ${ValueWithPostCompute::class.simpleName} recursively.")
            }
            else -> {
                // other thread computed the value for us and set `lock` to null
                require(lock == null)
                return value as VALUE
            }
        }

        val calculatedValue = try {
            value = ValueIsCalculatingNow
            val (calculated, data) = _calculate!!(key)
            value = ValueIsPostComputingNow(calculated, Thread.currentThread().id) // only current thread may see the value
            _postCompute!!(key, calculated, data)
            calculated
        } catch (e: Throwable) {
            value = ValueIsNotComputed
            throw e
        }
        // reading lock = null implies that the value is calculated and stored
        value = calculatedValue
        _calculate = null
        _postCompute = null
        lock = null

        return calculatedValue
    }

    @Suppress("UNCHECKED_CAST")
    fun getValueIfComputed(): VALUE? = when (value) {
        ValueIsNotComputed, ValueIsCalculatingNow -> null
        is ValueIsPostComputingNow -> null
        else -> value as VALUE
    }

    private object ValueIsNotComputed

    /**
     * [ValueWithPostCompute] is in the [ValueIsCalculatingNow] state during value calculation with [_calculate]. It allows us to detect
     * forbidden recursive calls to [getValue] during value calculation, as we can throw an error if [computeValueWithoutLock] is entered
     * again while the value is in a [ValueIsCalculatingNow] state.
     *
     * We cannot detect recursion from the [lock]'s hold count since it may be shared and a value may be calculated inside the
     * post-computation of another [ValueWithPostCompute].
     */
    private object ValueIsCalculatingNow

    private class ValueIsPostComputingNow(val value: Any?, val threadId: Long)
}

private const val LOCKING_INTERVAL_MS = 50L