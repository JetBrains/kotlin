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

package org.jetbrains.jet.lang.resolve.lazy;

import com.intellij.openapi.util.Computable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class LockBasedStorageManager implements StorageManager {

    private final Object lock = new Object() {
        @Override
        public String toString() {
            return "LockBasedStorageManager centralized lock";
        }
    };

    @NotNull
    @Override
    public <K, V> ConcurrentMap<K, V> createConcurrentMap() {
        return new ConcurrentHashMap<K, V>();
    }

    @NotNull
    @Override
    public <T> LazyValue<T> createLazyValue(@NotNull Computable<T> computable) {
        return new LockBasedLazyValue<T>(lock, computable);
    }

    private static class LockBasedLazyValue<T> implements LazyValue<T> {
        private final Object lock;
        private final Computable<T> computable;

        @Nullable
        private volatile T value;

        public LockBasedLazyValue(@NotNull Object lock, @NotNull Computable<T> computable) {
            this.lock = lock;
            this.computable = computable;
        }

        @NotNull
        @Override
        public T get() {
            T _value = value;
            if (_value != null) {
                return _value;
            }

            synchronized (lock) {
                _value = value;
                if (_value == null) {
                    value = computable.compute();
                    _value = value;
                }
                return _value;
            }
        }
    }
}
