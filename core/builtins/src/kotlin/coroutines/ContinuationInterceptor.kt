/*
 * Copyright 2010-2017 JetBrains s.r.o.
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
 * Marks coroutine context element that intercepts coroutine continuations.
 */
@SinceKotlin("1.1")
public interface ContinuationInterceptor : CoroutineContextElement {
    /**
     * The key that defines *the* context interceptor.
     */
    companion object : CoroutineContextKey<ContinuationInterceptor>

    /**
     * Intercepts the given [continuation] by wrapping it. Application code should not call this method directly as
     * it is invoked by coroutines framework appropriately and the resulting continuations are efficiently cached.
     */
    public fun <T> interceptContinuation(continuation: Continuation<T>): Continuation<T>
}
