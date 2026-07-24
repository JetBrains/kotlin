/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.intellij.util.containers;

import com.intellij.util.ReflectionUtil;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.util.Objects;

/**
 * Internal utility class enabling the usage of VarHandle-like API in pre/after-jdk9 environments,
 * with the ability to switch the implementations dynamically.
 * It's used for porting classes containing VarHandles to jdk8-only modules, like util
 */
@SuppressWarnings({"unused", "UnstableApiUsage"})
@ApiStatus.Internal
public abstract class VarHandleWrapper {
    @SuppressWarnings("WeakerAccess") protected static VarHandleWrapperFactory FACTORY;

    public static @NotNull VarHandleWrapperFactory getFactory() {
        VarHandleWrapperFactory factory = FACTORY;
        if (factory == null) {
            // try VarHandles first; if not available, use Unsafe
            try {
                Objects.requireNonNull(ReflectionUtil.getMethod(Class.forName("com.intellij.concurrency.VarHandleWrapperImpl"), "useVarHandlesInConcurrentCollections"))
                        .invoke(null);
                if (FACTORY == null) {
                    throw new ClassNotFoundException("VarHandleWrapperImpl is found but factory is still uninitialized (loader problem?)");
                }
            }
            catch (InvocationTargetException | ClassNotFoundException e) {
                VarHandleWrapperUnsafe.useUnsafeInConcurrentCollections();
            }
            catch (IllegalAccessException e) {
                // signature was broken
                throw new RuntimeException(e);
            }
            factory = FACTORY;
        }
        return factory;
    }

    public interface VarHandleWrapperFactory {
        @NotNull VarHandleWrapper create(@NotNull Class<?> containingClass, @NotNull String name, @NotNull Class<?> type);
        @NotNull VarHandleWrapper createForArrayElement(@NotNull Class<?> arrayClass);
    }

    public abstract Object getVolatile(Object thisObject);
    public abstract void setVolatile(Object thisObject, Object value);

    public abstract boolean compareAndSet(Object thisObject, Object expected, Object actual);
    public abstract boolean compareAndSetInt(Object thisObject, int expected, int actual);
    public abstract boolean compareAndSetLong(Object thisObject, long expected, long actual);
    public abstract boolean compareAndSetByte(Object thisObject, byte expected, byte actual);
    public abstract int getAndAdd(Object thisObject, int value);

    public abstract Object getVolatileArrayElement(Object thisObject, int index);
    public abstract void setVolatileArrayElement(Object thisObject, int index, Object value);
    public abstract boolean compareAndSetArrayElement(Object thisObject, int index, Object expected, Object value);
}
