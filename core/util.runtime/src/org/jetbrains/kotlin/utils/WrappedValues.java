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

package org.jetbrains.kotlin.utils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class WrappedValues {
    private static final Object NULL_VALUE = new Object() {
        @Override
        public String toString() {
            return "NULL_VALUE";
        }
    };
    public static volatile boolean throwWrappedProcessCanceledException = false;

    private final static class ThrowableWrapper {
        private final Throwable throwable;

        private ThrowableWrapper(@NotNull Throwable throwable) {
            this.throwable = throwable;
        }

        @NotNull
        public Throwable getThrowable() {
            return throwable;
        }

        @Override
        public String toString() {
            return throwable.toString();
        }
    }

    private WrappedValues() {
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public static <V> V unescapeNull(@NotNull Object value) {
        if (value == NULL_VALUE) return null;
        return (V) value;
    }

    @NotNull
    public static <V> Object escapeNull(@Nullable V value) {
        if (value == null) return NULL_VALUE;
        return value;
    }

    @NotNull
    public static Object escapeThrowable(@NotNull Throwable throwable) {
        return new ThrowableWrapper(throwable);
    }

    @Nullable
    public static <V> V unescapeExceptionOrNull(@NotNull Object value) {
        return unescapeNull(unescapeThrowable(value));
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public static <V> V unescapeThrowable(@Nullable Object value) {
        if (value instanceof ThrowableWrapper) {
            Throwable originThrowable = ((ThrowableWrapper) value).getThrowable();

            if (throwWrappedProcessCanceledException && ExceptionUtilsKt.isProcessCanceledException(originThrowable)) {
                throw new WrappedProcessCanceledException(originThrowable);
            }

            throw ExceptionUtilsKt.rethrow(originThrowable);
        }

        return (V) value;
    }

    public static class WrappedProcessCanceledException extends RuntimeException {
        public WrappedProcessCanceledException(Throwable cause) {
            super("Rethrow stored exception", cause);
        }
    }
}
