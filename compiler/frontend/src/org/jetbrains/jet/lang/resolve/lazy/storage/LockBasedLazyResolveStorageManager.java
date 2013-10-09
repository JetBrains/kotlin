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
import com.intellij.util.containers.ConcurrentWeakValueHashMap;
import jet.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.Diagnostics;
import org.jetbrains.jet.storage.LockBasedStorageManager;
import org.jetbrains.jet.storage.MemoizedFunctionToNotNull;
import org.jetbrains.jet.storage.MemoizedFunctionToNullable;
import org.jetbrains.jet.util.slicedmap.ReadOnlySlice;
import org.jetbrains.jet.util.slicedmap.WritableSlice;

import java.util.Collection;
import java.util.concurrent.locks.Lock;

public class LockBasedLazyResolveStorageManager extends LockBasedStorageManager implements LazyResolveStorageManager {

    @Override
    @NotNull
    public <K, V> MemoizedFunctionToNotNull<K, V> createWeaklyRetainedMemoizedFunction(
            @NotNull Function1<K, V> compute
    ) {
        return super.createMemoizedFunction(compute, new ConcurrentWeakValueHashMap<K, Object>());
    }

    @NotNull
    @Override
    public <K, V> MemoizedFunctionToNullable<K, V> createWeaklyRetainedMemoizedFunctionWithNullableValues(
            @NotNull Function1<K, V> compute
    ) {
        return super.createMemoizedFunctionWithNullableValues(compute, new ConcurrentWeakValueHashMap<K, Object>());
    }

    @NotNull
    @Override
    public BindingTrace createSafeTrace(@NotNull BindingTrace originalTrace) {
        // It seems safe to have a separate lock for traces:
        // no other locks will be acquired inside the trace operations
        return new LockProtectedTrace(lock, originalTrace);
    }

    private static class LockProtectedContext implements BindingContext {
        private final Lock lock;
        private final BindingContext context;

        private LockProtectedContext(Lock lock, BindingContext context) {
            this.lock = lock;
            this.context = context;
        }

        @NotNull
        @Override
        public Diagnostics getDiagnostics() {
            lock.lock();
            try {
                return context.getDiagnostics();
            }
            finally {
                lock.unlock();
            }
        }

        @Nullable
        @Override
        public <K, V> V get(ReadOnlySlice<K, V> slice, K key) {
            lock.lock();
            try {
                return context.get(slice, key);
            }
            finally {
                lock.unlock();
            }
        }

        @NotNull
        @Override
        public <K, V> Collection<K> getKeys(WritableSlice<K, V> slice) {
            lock.lock();
            try {
                return context.getKeys(slice);
            }
            finally {
                lock.unlock();
            }
        }

        @NotNull
        @Override
        @TestOnly
        public <K, V> ImmutableMap<K, V> getSliceContents(@NotNull ReadOnlySlice<K, V> slice) {
            lock.lock();
            try {
                return context.getSliceContents(slice);
            }
            finally {
                lock.unlock();
            }
        }
    }

    private static class LockProtectedTrace implements BindingTrace {
        private final Lock lock;
        private final BindingTrace trace;
        private final BindingContext context;

        public LockProtectedTrace(@NotNull Lock lock, @NotNull BindingTrace trace) {
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
            lock.lock();
            try {
                trace.record(slice, key, value);
            }
            finally {
                lock.unlock();
            }
        }

        @Override
        public <K> void record(WritableSlice<K, Boolean> slice, K key) {
            lock.lock();
            try {
                trace.record(slice, key);
            }
            finally {
                lock.unlock();
            }
        }

        @Override
        @Nullable
        public <K, V> V get(ReadOnlySlice<K, V> slice, K key) {
            lock.lock();
            try {
                return trace.get(slice, key);
            }
            finally {
                lock.unlock();
            }
        }

        @Override
        @NotNull
        public <K, V> Collection<K> getKeys(WritableSlice<K, V> slice) {
            lock.lock();
            try {
                return trace.getKeys(slice);
            }
            finally {
                lock.unlock();
            }
        }

        @Override
        public void report(@NotNull Diagnostic diagnostic) {
            lock.lock();
            try {
                trace.report(diagnostic);
            }
            finally {
                lock.unlock();
            }
        }
    }
}
