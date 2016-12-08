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
 * Converts a callable references to `suspend` lambda into a tail-callable suspend function.
 * It can be used to define arbitrary suspension function without tail-call restrictions, for example
 *
 * ```
 * suspend fun awaitTwice(...): R = suspendable<R> {
 *     await(...) // calls one suspend function
 *     await(...) // calls another suspend function
 * }
 */
@SinceKotlin("1.1")
suspend fun <T> suspendable(/*suspend*/ lambda: () -> T): T = suspendWithCurrentContinuation<T> { c ->
    @Suppress("UNCHECKED_CAST")
    (lambda as Function1<Continuation<T>, Any?>).invoke(c)
}
