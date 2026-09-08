/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.intellij.util.io;

import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;

// This file was hijacked from com.jetbrains.intellij.platform:util to prevent problems with
// finding jdk.internal.ref.Cleaner class, that is inaccessible from JDK 9+.
// In the platform, it's referenced indirectly from this class static initializer,
// so we had either to hijack and then to drop this reference,
// or to use --add-opens java.base/jdk.internal.ref=ALL-UNNAMED everywhere that seems unreliable.
// When this class will be revised in the platform, we can drop this file and use platform's one instead.

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
