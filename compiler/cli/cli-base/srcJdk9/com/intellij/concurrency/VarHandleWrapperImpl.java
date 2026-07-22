/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.intellij.concurrency;

import com.intellij.util.containers.VarHandleWrapper;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

/**
 * Implementation of {@link VarHandleWrapper} based on {@link VarHandle}, when the latter is available in the classpath
 */
@SuppressWarnings({"unused", "UnstableApiUsage"})
@ApiStatus.Internal
public class VarHandleWrapperImpl extends VarHandleWrapper implements VarHandleWrapper.VarHandleWrapperFactory {
    private final VarHandle myVarHandle;
    private final boolean isArray;

    private VarHandleWrapperImpl(VarHandle varHandle, boolean isArray) {
        myVarHandle = varHandle;
        this.isArray = isArray;
    }

    @Override
    public @NotNull VarHandleWrapper create(@NotNull Class<?> containingClass, @NotNull String name, @NotNull Class<?> type) {
        try {
            VarHandle handle = MethodHandles
                    .privateLookupIn(containingClass, MethodHandles.lookup())
                    .findVarHandle(containingClass, name, type);

            return new VarHandleWrapperImpl(handle, false);
        }
        catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public @NotNull VarHandleWrapper createForArrayElement(@NotNull Class<?> arrayClass) {
        assert arrayClass.isArray();
        return new VarHandleWrapperImpl(MethodHandles.arrayElementVarHandle(arrayClass), true);
    }

    //@Override
    public Object getVolatile(Object thisObject) {
        assert !isArray;
        return myVarHandle.getVolatile(thisObject);
    }

    //@Override
    public void setVolatile(Object thisObject, Object value) {
        assert !isArray;
        myVarHandle.setVolatile(thisObject, value);
    }

    @Override
    public boolean compareAndSet(Object thisObject, Object expected, Object actual) {
        assert !isArray;
        //assert !myVarHandle.varType().isPrimitive() : myVarHandle;
        return myVarHandle.compareAndSet(thisObject, expected, actual);
    }

    @Override
    public boolean compareAndSetInt(Object thisObject, int expected, int actual) {
        assert !isArray;
        //assert myVarHandle.varType() == int.class : myVarHandle;
        return myVarHandle.compareAndSet(thisObject, expected, actual);
    }

    @Override
    public boolean compareAndSetByte(Object thisObject, byte expected, byte actual) {
        assert !isArray;
        //assert myVarHandle.varType() == byte.class : myVarHandle;
        return myVarHandle.compareAndSet(thisObject, expected, actual);
    }

    @Override
    public boolean compareAndSetLong(Object thisObject, long expected, long actual) {
        assert !isArray;
        //assert myVarHandle.varType() == long.class : myVarHandle;
        return myVarHandle.compareAndSet(thisObject, expected, actual);
    }

    @Override
    public Object getVolatileArrayElement(Object thisObject, int index) {
        assert isArray;
        return myVarHandle.getVolatile(thisObject, index);
    }

    @Override
    public void setVolatileArrayElement(Object thisObject, int index, Object value) {
        assert isArray;
        myVarHandle.setVolatile(thisObject, index, value);
    }

    @Override
    public boolean compareAndSetArrayElement(Object thisObject, int index, Object expected, Object value) {
        assert isArray;
        return myVarHandle.compareAndSet(thisObject, index, expected, value);
    }

    @Override
    public int getAndAdd(Object thisObject, int value) {
        assert !isArray;
        //assert myVarHandle.varType() == int.class : myVarHandle;
        return (int)myVarHandle.getAndAdd(thisObject, value);
    }

    public static void useVarHandlesInConcurrentCollections() {
        // use VarHandles in concurrent collections because they are available in the classpath
        FACTORY = new VarHandleWrapperImpl(null, false);
    }
}
