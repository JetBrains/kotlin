/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.intellij.util.io;

import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;

@SuppressWarnings("unused")
public final class ByteBufferUtil {
    private static final Logger LOG = Logger.getInstance(ByteBufferUtil.class);

    /**
     * Please use with care. In most cases leaving the job to the GC is enough.
     */
    public static boolean cleanBuffer(@NotNull ByteBuffer buffer) {
        return true;
    }

    public static void copyMemory(@NotNull ByteBuffer src, int index, byte[] dst, int dstIndex, int length) {
        ByteBuffer buf = src.duplicate();
        buf.position(index);
        buf.get(dst, dstIndex, length);
    }
}
