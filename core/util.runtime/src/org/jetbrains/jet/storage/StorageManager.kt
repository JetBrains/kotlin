/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.storage


public trait StorageManager {
    /**
     * Given a function compute: K -> V create a memoized version of it that computes a value only once for each key
     * @param compute the function to be memoized
     * @param valuesReferenceKind how to store the memoized values
     *
     * NOTE: if compute() has side-effects the WEAK reference kind is dangerous: the side-effects will be repeated if
     *       the value gets collected and then re-computed
     */
    public fun createMemoizedFunction<K, V: Any>(compute: (K) -> V): MemoizedFunctionToNotNull<K, V>

    public fun createMemoizedFunctionWithNullableValues<K, V: Any>(compute: (K) -> V?): MemoizedFunctionToNullable<K, V>

    public fun createLazyValue<T: Any>(computable: () -> T): NotNullLazyValue<T>

    public fun createRecursionTolerantLazyValue<T: Any>(computable: () -> T, onRecursiveCall: T): NotNullLazyValue<T>

    /**
     * @param onRecursiveCall is called if the computation calls itself recursively.
     *                        The parameter to it is {@code true} for the first call, {@code false} otherwise.
     *                        If {@code onRecursiveCall} is {@code null}, an exception will be thrown on a recursive call,
     *                        otherwise it's executed and its result is returned
     * @param postCompute is called after the value is computed, but before any other thread sees it
     */
    public fun createLazyValueWithPostCompute<T: Any>(computable: () -> T, onRecursiveCall: ((Boolean) -> T)?, postCompute: (T) -> Unit): NotNullLazyValue<T>

    public fun createNullableLazyValue<T: Any>(computable: () -> T?): NullableLazyValue<T>

    public fun createRecursionTolerantNullableLazyValue<T: Any>(computable: () -> T?, onRecursiveCall: T?): NullableLazyValue<T>

    /**
     * {@code postCompute} is called after the value is computed, but before any other thread sees it (the current thread may
     * see it in between)
     */
    public fun createNullableLazyValueWithPostCompute<T: Any>(computable: () -> T?, postCompute: (T?) -> Unit): NullableLazyValue<T>

    public fun compute<T>(computable: () -> T): T
}
