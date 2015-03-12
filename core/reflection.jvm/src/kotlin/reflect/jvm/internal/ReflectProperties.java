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

package kotlin.reflect.jvm.internal;

import kotlin.Function0;
import org.jetbrains.annotations.NotNull;

import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;

/* package */ class ReflectProperties {
    public static abstract class Val<T> {
        private static final Object NULL_VALUE = new Object() {};

        public abstract T get(Object instance, Object metadata);

        protected Object escape(T value) {
            return value == null ? NULL_VALUE : value;
        }

        @SuppressWarnings("unchecked")
        protected T unescape(Object value) {
            return value == NULL_VALUE ? null : (T) value;
        }
    }

    // A delegate for a lazy property, whose initializer may be invoked multiple times including simultaneously from different threads
    public static class LazyVal<T> extends Val<T> {
        private final Function0<T> initializer;
        private Object value = null;

        public LazyVal(@NotNull Function0<T> initializer) {
            this.initializer = initializer;
        }

        @Override
        public T get(Object instance, Object metadata) {
            Object cached = value;
            if (cached != null) {
                return unescape(cached);
            }

            T result = initializer.invoke();
            value = escape(result);

            return result;
        }
    }

    // A delegate for a lazy property on a soft reference, whose initializer may be invoked multiple times
    // including simultaneously from different threads
    public static class LazySoftVal<T> extends Val<T> {
        private final Function0<T> initializer;
        private SoftReference<Object> value = null;

        public LazySoftVal(@NotNull Function0<T> initializer) {
            this.initializer = initializer;
        }

        @Override
        public T get(Object instance, Object metadata) {
            SoftReference<Object> cached = value;
            if (cached != null) {
                Object result = cached.get();
                if (result != null) {
                    return unescape(result);
                }
            }

            T result = initializer.invoke();
            value = new SoftReference<Object>(escape(result));

            return result;
        }
    }

    // A delegate for a lazy property on a weak reference, whose initializer may be invoked multiple times
    // including simultaneously from different threads
    public static class LazyWeakVal<T> extends Val<T> {
        private final Function0<T> initializer;
        private WeakReference<Object> value = null;

        public LazyWeakVal(@NotNull Function0<T> initializer) {
            this.initializer = initializer;
        }

        @Override
        public T get(Object instance, Object metadata) {
            WeakReference<Object> cached = value;
            if (cached != null) {
                Object result = cached.get();
                if (result != null) {
                    return unescape(result);
                }
            }

            T result = initializer.invoke();
            value = new WeakReference<Object>(escape(result));

            return result;
        }
    }

    @NotNull
    public static <T> LazyVal<T> lazy(@NotNull Function0<T> initializer) {
        return new LazyVal<T>(initializer);
    }

    @NotNull
    public static <T> LazySoftVal<T> lazySoft(@NotNull Function0<T> initializer) {
        return new LazySoftVal<T>(initializer);
    }

    @NotNull
    public static <T> LazyWeakVal<T> lazyWeak(@NotNull Function0<T> initializer) {
        return new LazyWeakVal<T>(initializer);
    }
}
