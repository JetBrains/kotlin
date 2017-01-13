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
 * Persistent context for the coroutine. It is an indexed set of [CoroutineContextElement] instances.
 * An indexed set is a mix between a set and a map.
 * Every element in this set has a unique [CoroutineContextKey].
 */
@SinceKotlin("1.1")
public interface CoroutineContext {
    /**
     * Returns an element with the given [key] in this context or `null`.
     */
    public operator fun <E : CoroutineContextElement> get(key: CoroutineContextKey<E>): E?

    /**
     * Accumulates entries of this context starting with [initial] value and applying [operation]
     * from left to right to current accumulator value and each element of this context.
     */
    public fun <R> fold(initial: R, operation: (R, CoroutineContextElement) -> R): R

    /**
     * Returns a context containing elements from this context and elements from other [context].
     * The elements from this context with the same key as in the other one are dropped.
     */
    public operator fun plus(context: CoroutineContext): CoroutineContext

    /**
     * Returns a context containing elements from this context, but without an element with
     * the specified [key].
     */
    public fun minusKey(key: CoroutineContextKey<*>): CoroutineContext
}

/**
 * An element of the [CoroutineContext]. An element of the coroutine context is a singleton context by itself.
 */
@SinceKotlin("1.1")
public interface CoroutineContextElement : CoroutineContext {
    /**
     * A key of this coroutine context element.
     */
    public val contextKey: CoroutineContextKey<*>
}

/**
 * Key for the elements of [CoroutineContext]. [E] is a type of the element with this key.
 */
@SinceKotlin("1.1")
public interface CoroutineContextKey<E : CoroutineContextElement>

