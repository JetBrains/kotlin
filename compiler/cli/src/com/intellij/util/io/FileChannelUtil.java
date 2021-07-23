// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.io;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.ExceptionUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.channels.FileChannel;

final class FileChannelUtil {
  private static final Logger LOG = Logger.getInstance(FileChannelUtil.class);

  private static final Class<?> sunNioChFileChannelImpl = setupFileChannelImpl();
  private static final MethodHandle setUnInterruptible = setupUnInterruptibleHandle();

  private static Class<?> setupFileChannelImpl() {
    try {
      return Class.forName("sun.nio.ch.FileChannelImpl");
    }
    catch (ClassNotFoundException ignored) {}
    return null;
  }

  @Nullable
  private static MethodHandle setupUnInterruptibleHandle() {
    MethodHandle setUnInterruptible = null;
    try {
      if (sunNioChFileChannelImpl != null) {
        // noinspection SpellCheckingInspection
        setUnInterruptible = MethodHandles
          .lookup()
          .findVirtual(sunNioChFileChannelImpl, "setUninterruptible", MethodType.methodType(void.class));
      }
    }
    catch (NoSuchMethodException e) {
      LOG.info("interruptible FileChannels will be used for indexes");
    }
    catch (IllegalAccessException e) {
      LOG.error(e);
    }
    if (setUnInterruptible != null) {
      LOG.info("uninterruptible FileChannels will be used for indexes");
    }
    else {
      LOG.info("interruptible FileChannels will be used for indexes");
    }
    return setUnInterruptible;
  }

  @NotNull
  static FileChannel unInterruptible(@NotNull FileChannel channel) {
    try {
      if (setUnInterruptible != null && sunNioChFileChannelImpl != null && sunNioChFileChannelImpl.isInstance(channel)) {
        setUnInterruptible.invoke(channel);
      }
    }
    catch (Throwable e) {
      ExceptionUtil.rethrow(e);
    }
    return channel;
  }
}
