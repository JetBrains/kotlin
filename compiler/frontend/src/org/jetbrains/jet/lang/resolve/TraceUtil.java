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

package org.jetbrains.jet.lang.resolve;

import com.google.common.collect.ImmutableMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.util.slicedmap.ReadOnlySlice;
import org.jetbrains.jet.util.slicedmap.WritableSlice;

import java.util.Collection;

public class TraceUtil {
    public final static BindingTrace TRACE_STUB = new BindingTrace() {

        @Override
        public BindingContext getBindingContext() {
            return BINDING_CONTEXT_STUB;
        }

        @Override
        public <K, V> void record(WritableSlice<K, V> slice, K key, V value) {
            throw new IllegalStateException();
        }

        @Override
        public <K> void record(WritableSlice<K, Boolean> slice, K key) {
            throw new IllegalStateException();
        }

        @Nullable
        @Override
        public <K, V> V get(ReadOnlySlice<K, V> slice, K key) {
            throw new IllegalStateException();
        }

        @NotNull
        @Override
        public <K, V> Collection<K> getKeys(WritableSlice<K, V> slice) {
            throw new IllegalStateException();
        }

        @Override
        public void report(@NotNull Diagnostic diagnostic) {
            throw new IllegalStateException();
        }
    };

    public final static BindingContext BINDING_CONTEXT_STUB = new BindingContext() {
        @Override
        public Collection<Diagnostic> getDiagnostics() {
            throw new IllegalStateException();
        }

        @Nullable
        @Override
        public <K, V> V get(ReadOnlySlice<K, V> slice, K key) {
            throw new IllegalStateException();
        }

        @NotNull
        @Override
        public <K, V> Collection<K> getKeys(WritableSlice<K, V> slice) {
            throw new IllegalStateException();
        }

        @NotNull
        @Override
        public <K, V> ImmutableMap<K, V> getSliceContents(@NotNull ReadOnlySlice<K, V> slice) {
            throw new IllegalStateException();
        }
    };

    public final static DelegatingBindingTrace DELEGATING_TRACE_STUB = new DelegatingBindingTrace(BINDING_CONTEXT_STUB, "Delegating trace stub") {

        @Override
        public <K, V> void record(WritableSlice<K, V> slice, K key, V value) {
            throw new IllegalStateException();
        }

        @Override
        public <K> void record(WritableSlice<K, Boolean> slice, K key) {
            throw new IllegalStateException();
        }

        @Nullable
        @Override
        public <K, V> V get(ReadOnlySlice<K, V> slice, K key) {
            throw new IllegalStateException();
        }

        @NotNull
        @Override
        public <K, V> Collection<K> getKeys(WritableSlice<K, V> slice) {
            throw new IllegalStateException();
        }

        @Override
        public void report(@NotNull Diagnostic diagnostic) {
            throw new IllegalStateException();
        }
    };
}
