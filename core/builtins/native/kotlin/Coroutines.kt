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
 * This function allows to obtain the current continuation instance inside suspend functions and either suspend
 * currently running  coroutine or return result immediately without suspension.
 * This function can be used in a tail-call position as the return value of another suspend function.
 *
 * If the [body] returns the special [SUSPENDED_COMPUTATION] value, it means that suspend function did suspend the execution and will
 * not return any result immediately. In this case, the [Continuation] provided to the [body] shall be invoked at some moment in the future when
 * the result becomes available to resume the computation.
 *
 * Otherwise, the return value of the [body] must have a type assignable to [T] and represents the result of this suspend function.
 * It means that the execution was not suspended and the [Continuation] provided to the [body] shall not be invoked.
 * As the result type of the [body] is declared as `Any?` and cannot be correctly type-checked,
 * its proper return type remains on the conscience of the suspend function's author.
 *
 * Note that it is not recommended to call either [Continuation.resume] nor [Continuation.resumeWithException] functions synchronously in
 * the same stackframe where suspension function is run. They should be called asynchronously either later in the same thread or
 * from a different thread of execution.
 * Repeated invocation of any resume function on continuation produces unspecified behavior.
 * Use [runWithCurrentContinuation] as a safer way to obtain current continuation instance.
 */
@SinceKotlin("1.1")
public inline suspend fun <T> maySuspendWithCurrentContinuation(body: (Continuation<T>) -> Any?): T
