/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kotlin.coroutines

import java.lang.IllegalStateException
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater

/**
 * This function allows to obtain the current continuation instance inside suspend functions and suspend
 * currently running coroutine.
 * This function can be used in a tail-call position as the return value of another suspend function.
 *
 * Note that it is not recommended to call either [Continuation.resume] nor [Continuation.resumeWithException] functions synchronously in
 * the same stackframe where suspension function is run. They should be called asynchronously either later in the same thread or
 * from a different thread of execution.
 * In practise, this restriction means that only _asynchronous_ promises and callbacks can be used to resume the continuation
 * in this function.
 * Repeated invocation of any resume function on continuation produces unspecified behavior.
 * Use [runWithCurrentContinuation] as a safer way to obtain current continuation instance.
 */
//@SinceKotlin("1.1")
//public inline suspend fun <T> suspendWithCurrentContinuation(crossinline body: (Continuation<T>) -> Unit): T =
//    maySuspendWithCurrentContinuation<T> { c: Continuation<T> ->
//        body(c)
//        SUSPENDED
//    }

/**
 * This function allows to safely obtain the current continuation instance inside suspend functions and suspend
 * currently running coroutine.
 * This function can be used in a tail-call position as the return value of another suspend function.
 *
 * In this function both [Continuation.resume] and [Continuation.resumeWithException] can be used either synchronously in
 * the same stackframe where suspension function is run or asynchronously later in the same thread or
 * from a different thread of execution.
 * Repeated invocation of any resume function produces [IllegalStateException].
 */
@SinceKotlin("1.1")
public inline suspend fun <T> runWithCurrentContinuation(crossinline body: (Continuation<T>) -> Unit): T =
    suspendWithCurrentContinuation { c: Continuation<T> ->
        val safe = SafeContinuation(c)
        body(safe)
        safe.getResult()
    }

private val UNDECIDED: Any? = Any()
private val RESUMED: Any? = Any()
private class Fail(val exception: Throwable)

@Suppress("UNCHECKED_CAST")
private val RESULT_UPDATER = AtomicReferenceFieldUpdater.newUpdater<SafeContinuation<*>, Any?>(
        SafeContinuation::class.java, Any::class.java as Class<Any?>, "result")

@PublishedApi
internal class SafeContinuation<T> @PublishedApi internal constructor(private val delegate: Continuation<T>) : Continuation<T> {
    @Volatile
    private var result: Any? = UNDECIDED

    private fun cas(expect: Any?, update: Any?): Boolean =
        RESULT_UPDATER.compareAndSet(this, expect, update)

    override fun resume(data: T) {
        while (true) { // lock-free loop
            val result = this.result // atomic read
            when (result) {
                UNDECIDED -> if (cas(UNDECIDED, data)) return
                SUSPENDED -> if (cas(SUSPENDED, RESUMED)) {
                    delegate.resume(data)
                    return
                }
                else -> throw IllegalStateException("Already resumed")
            }
        }
    }

    override fun resumeWithException(exception: Throwable) {
        while (true) { // lock-free loop
            val result = this.result // atomic read
            when (result) {
                UNDECIDED -> if (cas(UNDECIDED, Fail(exception))) return
                SUSPENDED -> if (cas(SUSPENDED, RESUMED)) {
                    delegate.resumeWithException(exception)
                    return
                }
                else -> throw IllegalStateException("Already resumed")
            }
        }
    }

    @PublishedApi
    internal fun getResult(): Any? {
        val result = this.result // atomic read
        if (result == UNDECIDED && cas(UNDECIDED, SUSPENDED)) return SUSPENDED
        when (result) {
            RESUMED -> return SUSPENDED // already called continuation, indicate SUSPENDED upstream
            is Fail -> throw result.exception
            else -> return result // either SUSPENDED or data
        }
    }
}

