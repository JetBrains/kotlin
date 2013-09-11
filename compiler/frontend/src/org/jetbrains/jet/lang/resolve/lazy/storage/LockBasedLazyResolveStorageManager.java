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

import com.google.common.collect.ImmutableMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.util.slicedmap.ReadOnlySlice;
import org.jetbrains.jet.util.slicedmap.WritableSlice;

import java.util.Collection;

public class LockBasedLazyResolveStorageManager extends LockBasedStorageManager implements LazyResolveStorageManager {
    @NotNull
    @Override
    public BindingTrace createSafeTrace(@NotNull BindingTrace originalTrace) {
        // It seems safe to have a separate lock for traces:
        // no other locks will be acquired inside the trace operations
        return new LockProtectedTrace(lock, originalTrace);
    }

    private static class LockProtectedContext implements BindingContext {
        private final Object lock;
        private final BindingContext context;

        private LockProtectedContext(Object lock, BindingContext context) {
            this.lock = lock;
            this.context = context;
        }

        @NotNull
        @Override
        public Collection<Diagnostic> getDiagnostics() {
            synchronized (lock) {
                return context.getDiagnostics();
            }
        }

        @Nullable
        @Override
        public <K, V> V get(ReadOnlySlice<K, V> slice, K key) {
            synchronized (lock) {
                return context.get(slice, key);
            }
        }

        @NotNull
        @Override
        public <K, V> Collection<K> getKeys(WritableSlice<K, V> slice) {
            synchronized (lock) {
                return context.getKeys(slice);
            }
        }

        @NotNull
        @Override
        @TestOnly
        public <K, V> ImmutableMap<K, V> getSliceContents(@NotNull ReadOnlySlice<K, V> slice) {
            synchronized (lock) {
                return context.getSliceContents(slice);
            }
        }
    }

    private static class LockProtectedTrace implements BindingTrace {
        private final Object lock;
        private final BindingTrace trace;
        private final BindingContext context;

        public LockProtectedTrace(@NotNull Object lock, @NotNull BindingTrace trace) {
            this.lock = lock;
            this.trace = trace;
            this.context = new LockProtectedContext(lock, trace.getBindingContext());
        }

        @Override
        public BindingContext getBindingContext() {
            return context;
        }

        @Override
        public <K, V> void record(WritableSlice<K, V> slice, K key, V value) {
            synchronized (lock) {
                trace.record(slice, key, value);
            }
        }

        @Override
        public <K> void record(WritableSlice<K, Boolean> slice, K key) {
            synchronized (lock) {
                trace.record(slice, key);
            }
        }

        @Override
        @Nullable
        public <K, V> V get(ReadOnlySlice<K, V> slice, K key) {
            synchronized (lock) {
                return trace.get(slice, key);
            }
        }

        @Override
        @NotNull
        public <K, V> Collection<K> getKeys(WritableSlice<K, V> slice) {
            synchronized (lock) {
                return trace.getKeys(slice);
            }
        }

        @Override
        public void report(@NotNull Diagnostic diagnostic) {
            synchronized (lock) {
                trace.report(diagnostic);
            }
        }
    }
}
