// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.openapi.util.objectTree;

import com.intellij.openapi.diagnostic.UntraceableException;
import com.intellij.openapi.util.Comparing;
import com.intellij.util.ExceptionUtilRt;
import com.intellij.util.containers.HashingStrategy;
import com.intellij.util.containers.Interner;
import com.intellij.util.containers.WeakInterner;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Objects;

/**
 * Please don't look, there's nothing interesting here.
 *
 *
 *
 *
 * If you insist, JVM stores stacktrace information in compact form in Throwable.backtrace field, but blocks reflective access to this field.
 * This class uses this field for comparing Throwables.
 * The available method Throwable.getStackTrace() unfortunately can't be used for that because it's
 * 1) too slow and 2) explodes Throwable retained size by polluting Throwable.stackTrace fields.
 */
public final class ThrowableInterner {
    private ThrowableInterner() {
    }

    private static final Interner<Throwable> myTraceInterner = new WeakInterner<>(new HashingStrategy<Throwable>() {
        @Override
        public int hashCode(Throwable throwable) {
            return computeHashCode(throwable);
        }

        @Override
        public boolean equals(Throwable o1, Throwable o2) {
            if (o1 == o2) return true;
            if (o1 == null || o2 == null) return false;

            if (!Comparing.equal(o1.getClass(), o2.getClass())) return false;
            if (!Objects.equals(o1.getMessage(), o2.getMessage())) return false;
            if (!equals(o1.getCause(), o2.getCause())) return false;
            Object[] backtrace1 = getBacktrace(o1);
            Object[] backtrace2 = getBacktrace(o2);
            if (backtrace1 != null && backtrace2 != null) {
                return Arrays.deepEquals(backtrace1, backtrace2);
            }
            return Arrays.equals(o1.getStackTrace(), o2.getStackTrace());
        }
    });

    private static int computeHashCode(@NotNull Throwable throwable) {
        int mHash;
        String message = throwable.getMessage();
        if (message == null) {
            mHash = 0;
        }
        else {
            mHash = message.hashCode() * 37;
        }
        return mHash + computeTraceHashCode(throwable);
    }

    public static int computeTraceHashCode(@NotNull Throwable throwable) {
        Object[] backtrace = getBacktrace(throwable);
        if (backtrace == null) {
            return Arrays.hashCode(throwable.getStackTrace());
        }

        for (Object element : backtrace) {
            if (element instanceof Object[]) {
                return Arrays.hashCode((Object[])element);
            }
        }
        return 0;
    }

    // more accurate hash code (different for different line numbers inside same method) but more expensive than computeTraceHashCode
    public static int computeAccurateTraceHashCode(@NotNull Throwable throwable) {
        Object[] backtrace = getBacktrace(throwable);
        if (backtrace == null) {
            StackTraceElement[] trace = throwable instanceof UntraceableException ? null : throwable.getStackTrace();
            return Arrays.hashCode(trace);
        }
        return Arrays.deepHashCode(backtrace);
    }

    private static final Field BACKTRACE_FIELD;

    static {
        Field backtraceField;
        try {
            backtraceField = Throwable.class.getDeclaredField("backtrace");
            backtraceField.setAccessible(true);
        }
        catch (NoSuchFieldException e) {
            backtraceField = null;
        }
        BACKTRACE_FIELD = backtraceField;
    }

    private static Object[] getBacktrace(@NotNull Throwable throwable) {
        // the JVM blocks access to Throwable.backtrace via reflection sometimes
        Object backtrace;
        try {
            backtrace = BACKTRACE_FIELD != null ? BACKTRACE_FIELD.get(throwable) : null;
        }
        catch (Throwable e) {
            return null;
        }
        // obsolete jdk
        return backtrace instanceof Object[] ? (Object[])backtrace : null;
    }

    public static void clearBacktrace(@NotNull Throwable throwable) {
        try {
            throwable.setStackTrace(new StackTraceElement[0]);
            if (BACKTRACE_FIELD != null) {
                BACKTRACE_FIELD.set(throwable, null);
            }
        }
        catch (Throwable e) {
            ExceptionUtilRt.rethrowUnchecked(e);
            throw new RuntimeException(e);
        }
    }

    public static @NotNull Throwable intern(@NotNull Throwable throwable) {
        return getBacktrace(throwable) == null ? throwable : myTraceInterner.intern(throwable);
    }

    public static void clearInternedBacktraces() {
        for (Throwable t : myTraceInterner.getValues()) {
            clearBacktrace(t);
        }
        myTraceInterner.clear();
    }
}
