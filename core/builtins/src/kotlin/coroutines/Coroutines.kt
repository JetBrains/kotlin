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

/**
 * Interface representing a continuation after a suspension point that returns value of type `T`.
 */
@SinceKotlin("1.1")
public interface Continuation<in T> {
    /**
     * Resumes the execution of the corresponding coroutine passing [value] as the return value of the last suspension point.
     */
    public fun resume(value: T)

    /**
     * Resumes the execution of the corresponding coroutine so that the [exception] is re-thrown right after the
     * last suspension point.
     */
    public fun resumeWithException(exception: Throwable)
}

/**
 * An interface to customise dispatch of continuations.
 */
@SinceKotlin("1.1")
public interface ContinuationDispatcher {
    /**
     * Dispatches [Continuation.resume] invocation.
     * This function must either return `false` or return `true` and invoke `continuation.resume(value)` asynchronously.
     */
    public fun <T> dispatchResume(value: T, continuation: Continuation<T>): Boolean = false

    /**
     * Dispatches [Continuation.resumeWithException] invocation.
     * This function must either return `false` or return `true` and invoke `continuation.resumeWithException(exception)` asynchronously.
     */
    public fun dispatchResumeWithException(exception: Throwable, continuation: Continuation<*>): Boolean = false
}

/**
 * Classes and interfaces marked with this annotation are restricted when used as receivers for extension
 * `suspend` functions. These `suspend` extensions can only invoke other member or extension `suspend` functions on this particular
 * receiver only and are restricted from calling arbitrary suspension functions.
 */
@SinceKotlin("1.1")
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
public annotation class RestrictsSuspendExtensions

/**
 * Contains intrinsic functions for coroutines.
 */
@SinceKotlin("1.1")
public object CoroutineIntrinsics {
    /**
     * Obtains the current continuation instance inside suspend functions and either suspend
     * currently running coroutine or return result immediately without suspension.
     *
     * If the [block] returns the special [SUSPENDED] value, it means that suspend function did suspend the execution and will
     * not return any result immediately. In this case, the [Continuation] provided to the [block] shall be invoked at some moment in the
     * future when the result becomes available to resume the computation.
     *
     * Otherwise, the return value of the [block] must have a type assignable to [T] and represents the result of this suspend function.
     * It means that the execution was not suspended and the [Continuation] provided to the [block] shall not be invoked.
     * As the result type of the [block] is declared as `Any?` and cannot be correctly type-checked,
     * its proper return type remains on the conscience of the suspend function's author.
     *
     * Note that it is not recommended to call either [Continuation.resume] nor [Continuation.resumeWithException] functions synchronously
     * in the same stackframe where suspension function is run. Use [suspendCoroutine] as a safer way to obtain current
     * continuation instance.
     */
    public inline suspend fun <T> suspendCoroutineOrReturn(
            block: (Continuation<T>) -> Any?
    ): T = suspendWithCurrentContinuation(block)

    /**
     * This value is used as a return value of [suspendCoroutineOrReturn] `block` argument to state that
     * the execution was suspended and will not return any result immediately.
     */
    @SinceKotlin("1.1")
    public val SUSPENDED: Any = Any()
}

@PublishedApi
internal inline suspend fun <T> suspendWithCurrentContinuation(body: (Continuation<T>) -> Any?): T = null!!
