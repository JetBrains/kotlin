/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.fir.caches

import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.IndexNotReadyException
import com.intellij.openapi.diagnostic.ControlFlowException
import org.jetbrains.kotlin.analysis.utils.errors.shouldIjPlatformExceptionBeRethrown

/**
 * Lazily calculated value which runs postCompute in the same thread,
 * assuming that postCompute may try to read that value inside current thread,
 * So in the period then value is calculated but post compute was not finished,
 * only thread that initiated the calculating may see the value,
 * other threads will have to wait wait until that value is calculated
 */
internal class ValueWithPostCompute<KEY, VALUE, DATA>(
    /**
     * We need at least one final field to be written in constructor to guarantee safe initialization of our [ValueWithPostCompute]
     */
    private val key: KEY,
    calculate: (KEY) -> Pair<VALUE, DATA>,
    postCompute: (KEY, VALUE, DATA) -> Unit,
) {
    private var _calculate: ((KEY) -> Pair<VALUE, DATA>)? = calculate
    private var _postCompute: ((KEY, VALUE, DATA) -> Unit)? = postCompute

    /**
     * can be in one of the following three states:
     * [ValueIsNotComputed] -- value is not initialized and thread are now executing [_postCompute]
     * [ExceptionWasThrownDuringValueComputation] -- exception was thrown during value computation, it will be rethrown on every value access
     * [ValueIsPostComputingNow] -- thread with threadId has computed the value and only it can access it during post compute
     * some value of type [VALUE] -- value is computed and post compute was executed, values is visible for all threads
     *
     * Value may be set only under [LazyValueWithPostCompute] intrinsic lock hold
     * And may be read from any thread
     */
    @Volatile
    private var value: Any? = ValueIsNotComputed

    private val guard = ThreadLocal.withInitial { false }
    private inline fun <T> recursiveGuarded(body: () -> T): T {
        check(!guard.get()) {
            "Should not be called recursively"
        }
        guard.set(true)
        return try {
            body()
        } finally {
            guard.set(false)
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun getValue(): VALUE {
        when (val stateSnapshot = value) {
            is ValueIsPostComputingNow -> {
                if (stateSnapshot.threadId == Thread.currentThread().id) {
                    return stateSnapshot.value as VALUE
                } else {
                    synchronized(this) { // wait until other thread which holds the lock now computes the value
                        return value as VALUE
                    }
                }
            }
            ValueIsNotComputed -> synchronized(this) {
                // if we entered synchronized section that's mean that the value is not yet calculated and was not started to be calculated
                // or the some other thread calculated the value while we were waiting to acquire the lock

                if (value != ValueIsNotComputed) { // some other thread calculated our value
                    return value as VALUE
                }
                val calculatedValue = try {
                    val (calculated, data) = recursiveGuarded {
                        _calculate!!(key)
                    }
                    value = ValueIsPostComputingNow(calculated, Thread.currentThread().id) // only current thread may see the value
                    _postCompute!!(key, calculated, data)
                    calculated
                } catch (e: Throwable) {
                    if (exceptionShouldBeSavedInCache(e)) {
                        value = ExceptionWasThrownDuringValueComputation(e)
                    } else {
                        value = ValueIsNotComputed
                    }
                    throw e
                }
                _calculate = null
                _postCompute = null
                value = calculatedValue
                return calculatedValue
            }
            is ExceptionWasThrownDuringValueComputation -> {
                throw stateSnapshot.error
            }
            else -> {
                return stateSnapshot as VALUE
            }
        }
    }

    private fun exceptionShouldBeSavedInCache(exception: Throwable): Boolean =
        !shouldIjPlatformExceptionBeRethrown(exception)


    @Suppress("UNCHECKED_CAST")
    fun getValueIfComputed(): VALUE? = when (val snapshot = value) {
        ValueIsNotComputed -> null
        is ValueIsPostComputingNow -> null
        is ExceptionWasThrownDuringValueComputation -> throw snapshot.error
        else -> value as VALUE
    }

    private class ValueIsPostComputingNow(val value: Any?, val threadId: Long)
    private class ExceptionWasThrownDuringValueComputation(val error: Throwable)
    private object ValueIsNotComputed
}