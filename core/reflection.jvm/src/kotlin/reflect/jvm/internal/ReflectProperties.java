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

import kotlin.jvm.functions.Function0;
import kotlin.properties.ReadOnlyProperty;
import kotlin.reflect.KProperty;
import kotlin.reflect.jvm.ReflectImplementation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.SoftReference;

public class ReflectProperties {
    public static interface InvokableReadOnlyProperty<T> extends ReadOnlyProperty<Object, T>, Function0<T> {}

    public static abstract class Val<T> implements InvokableReadOnlyProperty<T> {
        private static final Object NULL_VALUE = new Object() {
        };

        @Override
        public T getValue(Object thisRef, @NotNull KProperty<?> property) {
            return invoke();
        }

        protected Object escape(T value) {
            return value == null ? NULL_VALUE : value;
        }

        @SuppressWarnings("unchecked")
        protected T unescape(Object value) {
            return value == NULL_VALUE ? null : (T) value;
        }
    }

    // A delegate for a lazy property on a soft reference, whose initializer may be invoked multiple times
    // including simultaneously from different threads
    public static class LazySoftVal<T> extends Val<T> implements Function0<T> {
        private final Function0<T> initializer;
        private volatile SoftReference<Object> value = null;

        public LazySoftVal(@Nullable T initialValue, @NotNull Function0<T> initializer) {
            this.initializer = initializer;
            if (initialValue != null) {
                this.value = new SoftReference<Object>(escape(initialValue));
            }
        }

        @Override
        public T invoke() {
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

    @NotNull
    public static <T> LazySoftVal<T> lazySoft(@Nullable T initialValue, @NotNull Function0<T> initializer) {
        return new LazySoftVal<T>(initialValue, initializer);
    }

    @NotNull
    public static <T> InvokableReadOnlyProperty<T> lazySoft(@NotNull Function0<T> initializer) {
        if (ReflectImplementation.CACHING_ENABLED) {
            return new LazySoftVal<T>(null, initializer);
        }
        return new InvokableReadOnlyProperty<T>() {
            @Override
            public T getValue(Object thisRef, @NotNull KProperty<?> property) {
                return invoke();
            }

            @Override
            public T invoke() {
                return initializer.invoke();
            }
        };
    }
}
