// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.io;

import org.jetbrains.annotations.NotNull;

import java.nio.channels.FileChannel;

public final class FileChannelUtil {
    @NotNull
    static FileChannel unInterruptible(@NotNull FileChannel channel) {
        return channel;
    }
}
