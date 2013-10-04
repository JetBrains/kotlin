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

package org.jetbrains.jet.storage;

import jet.Function0;
import jet.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface StorageManager {
    /**
     * Given a function compute: K -> V create a memoized version of it that computes a value only once for each key
     * @param compute the function to be memoized
     * @param valuesReferenceKind how to store the memoized values
     *
     * NOTE: if compute() has side-effects the WEAK reference kind is dangerous: the side-effects will be repeated if
     */
    @NotNull
    <K, V> MemoizedFunctionToNotNull<K, V> createMemoizedFunction(@NotNull Function1<K, V> compute);
    @NotNull
    <K, V> MemoizedFunctionToNullable<K, V> createMemoizedFunctionWithNullableValues(@NotNull Function1<K, V> compute);

    @NotNull
    <T> NotNullLazyValue<T> createLazyValue(@NotNull Function0<T> computable);

    @NotNull
    <T> NotNullLazyValue<T> createRecursionTolerantLazyValue(@NotNull Function0<T> computable, @NotNull T onRecursiveCall);

    /**
     * @param computable
     * @param onRecursiveCall is called if the computation calls itself recursively.
     *                        The parameter to it is {@code true} for the first call, {@code false} otherwise.
     *                        If {@code onRecursiveCall} is {@code null}, an exception will be thrown on a recursive call,
     *                        otherwise it's executed and its result is returned
     * @param postCompute is called after the value is computed, but before any other thread sees it
     */
    @NotNull
    <T> NotNullLazyValue<T> createLazyValueWithPostCompute(
            @NotNull Function0<T> computable,
            @Nullable Function1<Boolean, T> onRecursiveCall,
            @NotNull Function1<T, Void> postCompute
    );

    @NotNull
    <T> NullableLazyValue<T> createNullableLazyValue(@NotNull Function0<T> computable);

    @NotNull
    <T> NullableLazyValue<T> createRecursionTolerantNullableLazyValue(@NotNull Function0<T> computable, @Nullable T onRecursiveCall);

    /**
     * {@code postCompute} is called after the value is computed, but before any other thread sees it (the current thread may
     * see it in between)
     */
    @NotNull
    <T> NullableLazyValue<T> createNullableLazyValueWithPostCompute(@NotNull Function0<T> computable, @NotNull Function1<T, Void> postCompute);

    <T> T compute(@NotNull Function0<T> computable);
}
