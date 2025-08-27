/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.intellij.openapi.application.impl;

import com.intellij.openapi.application.ex.ApplicationEx;
import com.intellij.openapi.application.ex.ApplicationManagerEx;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.awt.event.InvocationEvent;

public final class InvocationUtil {
  public static @Nullable Runnable extractRunnable(@NotNull AWTEvent event) {
    return null;
  }

  public static boolean replaceRunnable(@NotNull InvocationEvent event, @NotNull Runnable newRunnable) {
    return false;
  }

  @SuppressWarnings("deprecation")
  public static boolean priorityEventPending() {
    ApplicationEx app = ApplicationManagerEx.getApplicationEx();
    if (app != null) {
        //noinspection UnstableApiUsage
        app.flushNativeEventQueue();
    }
    AWTEvent event = Toolkit.getDefaultToolkit().getSystemEventQueue().peekEvent();
    return event != null && event.getClass().getName().equals("sun.awt.PeerEvent");
  }
}
