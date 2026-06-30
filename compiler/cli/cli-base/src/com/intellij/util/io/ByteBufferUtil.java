/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.intellij.util.io;

import com.intellij.ReviseWhenPortedToJDK;
import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.ByteBuffer;

// This file was hijacked from com.jetbrains.intellij.platform:util to prevent problems with
// finding jdk.internal.ref.Cleaner class, that is inaccessible from JDK 9+.
// In the platform, it's referenced indirectly from this class static initializer,
// so we had either to hijack and then to drop this reference,
// or to use --add-opens java.base/jdk.internal.ref=ALL-UNNAMED everywhere that seems unreliable.
// When this class will be revised in the platform, we can drop this file and use platform's one instead.

public final class ByteBufferUtil {
    private static final Logger LOG = Logger.getInstance(ByteBufferUtil.class);

    private static final MethodHandle cleanerHandle;
    private static final MethodHandle cleanerCleanHandle;

    /**
     * Please use with care. In most cases leaving the job to the GC is enough.
     */
    @ReviseWhenPortedToJDK("11")
    public static boolean cleanBuffer(@NotNull ByteBuffer buffer) {
        if (!buffer.isDirect()) return true;
        try {
            //noinspection JavaLangInvokeHandleSignature
            Object cleaner = cleanerHandle.invoke(buffer);
            if (cleaner != null) {
                cleanerCleanHandle.invoke(cleaner);
                return true;
            }
        }
        catch (Throwable e) {
            LOG.warn(e);
        }
        return false;
    }

    public static void copyMemory(@NotNull ByteBuffer src, int index, byte[] dst, int dstIndex, int length) {
        ByteBuffer buf = src.duplicate();
        buf.position(index);
        buf.get(dst, dstIndex, length);
    }

    static {
        try {
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            Class<?> directBufferClass = Class.forName("sun.nio.ch.DirectBuffer");
            Class<?> cleanerClass = directBufferClass.getDeclaredMethod("cleaner").getReturnType();
            cleanerHandle = lookup.findVirtual(directBufferClass, "cleaner", MethodType.methodType(cleanerClass));
            cleanerCleanHandle = lookup.findVirtual(cleanerClass, "clean", MethodType.methodType(Void.TYPE));
        }
        catch (Error | RuntimeException e) {
            throw e;
        }
        catch (Throwable e) {
            throw new IllegalStateException(e);
        }
    }
}
