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

package org.jetbrains.jet.lang.resolve.lazy.storage;

import com.intellij.openapi.util.Computable;
import com.intellij.util.Consumer;
import com.intellij.util.Function;
import org.jetbrains.annotations.NotNull;

public interface StorageManager {
    /**
     * Given a function compute: K -> V create a memoized version of it that computes a value only once for each key
     * @param compute the function to be memoized
     * @param valuesReferenceKind how to store the memoized values
     *
     * NOTE: if compute() has side-effects the WEAK reference kind is dangerous: the side-effects will be repeated if
     *       the value gets collected and then re-computed
     */
    @NotNull
    <K, V> MemoizedFunctionToNotNull<K, V> createMemoizedFunction(@NotNull Function<K, V> compute, @NotNull ReferenceKind valuesReferenceKind);
    @NotNull
    <K, V> MemoizedFunctionToNullable<K, V> createMemoizedFunctionWithNullableValues(@NotNull Function<K, V> compute, @NotNull ReferenceKind valuesReferenceKind);

    @NotNull
    <T> NotNullLazyValue<T> createLazyValue(@NotNull Computable<T> computable);

    /**
     * {@code postCompute} is called after the value is computed, but before any other thread sees it (the current thread may
     * see it in between)
     */
    @NotNull
    <T> NotNullLazyValue<T> createLazyValueWithPostCompute(@NotNull Computable<T> computable, @NotNull Consumer<T> postCompute);

    @NotNull
    <T> NullableLazyValue<T> createNullableLazyValue(@NotNull Computable<T> computable);

    /**
     * If recursion is detected, respective calls to compute() simply return null
     */
    @NotNull
    <T> NullableLazyValue<T> createRecursionTolerantNullableLazyValue(@NotNull Computable<T> computable);

    /**
     * {@code postCompute} is called after the value is computed, but before any other thread sees it (the current thread may
     * see it in between)
     */
    @NotNull
    <T> NullableLazyValue<T> createNullableLazyValueWithPostCompute(@NotNull Computable<T> computable, @NotNull Consumer<T> postCompute);

    <T> T compute(@NotNull Computable<T> computable);

    enum ReferenceKind {
        STRONG,
        WEAK
    }
}
