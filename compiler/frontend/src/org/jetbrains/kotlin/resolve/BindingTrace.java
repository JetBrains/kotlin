/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.resolve;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.diagnostics.DiagnosticSink;
import org.jetbrains.kotlin.psi.KtExpression;
import org.jetbrains.kotlin.types.KotlinType;
import org.jetbrains.kotlin.util.slicedMap.ReadOnlySlice;
import org.jetbrains.kotlin.util.slicedMap.WritableSlice;

import java.util.Collection;

public interface BindingTrace extends DiagnosticSink {

    @NotNull
    BindingContext getBindingContext();
    
    <K, V> void record(WritableSlice<K, V> slice, K key, V value);

    // Writes TRUE for a boolean value
    <K> void record(WritableSlice<K, Boolean> slice, K key);

    @Nullable
    <K, V> V get(ReadOnlySlice<K, V> slice, K key);

    // slice.isCollective() must be true
    @NotNull
    <K, V> Collection<K> getKeys(WritableSlice<K, V> slice);

    /**
     * Expression type should be taken from EXPRESSION_TYPE_INFO slice
     */
    @Nullable
    KotlinType getType(@NotNull KtExpression expression);

    /**
     * Expression type should be recorded into EXPRESSION_TYPE_INFO slice
     * (either updated old or a new one)
     */
    void recordType(@NotNull KtExpression expression, @Nullable KotlinType type);
}
