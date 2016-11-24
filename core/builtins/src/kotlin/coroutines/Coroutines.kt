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
 * Interface representing a continuation after a suspension point that returns value of type `P`
 */
@SinceKotlin("1.1")
public interface Continuation<in P> {
    /**
     * Resumes the execution of the corresponding coroutine passing `data` as the return value of the last suspension point
     */
    public fun resume(data: P)

    /**
     * Resumes the execution of the corresponding coroutine so that the `exception` is re-thrown right after the
     * last suspension point
     */
    public fun resumeWithException(exception: Throwable)
}


/**
 * Specifies that suspend extensions with a receiver based on corresponding controller class are allowed to be declared
 */
@SinceKotlin("1.1")
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
public annotation class AllowSuspendExtensions

/**
 * This object can be used as a return value of [kotlin.coroutines.suspendWithCurrentContinuation] lambda-argument to state that
 * the continuation will be resumed at some moment in the future, that means that suspend-function cannot return a value immediately,
 * i.e. it's literally suspends continuation, so no stack-unwinding will be performed by the continuation.
 */
@SinceKotlin("1.1")
public object Suspend
