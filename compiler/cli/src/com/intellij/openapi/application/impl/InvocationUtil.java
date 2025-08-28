// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.application.impl;

import com.intellij.openapi.application.ex.ApplicationEx;
import com.intellij.openapi.application.ex.ApplicationManagerEx;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InvocationEvent;
import java.lang.reflect.Field;

@ApiStatus.Internal
public final class InvocationUtil {
    public static final @NotNull Class<? extends Runnable> REPAINT_PROCESSING_CLASS = findProcessingClass();
    public static boolean isFlushNow(@NotNull Runnable runnable) {
        return LaterInvocator.isFlushNow(runnable);
    }
    private static final @NotNull Field INVOCATION_EVENT_RUNNABLE_FIELD = findRunnableField();

    private InvocationUtil() {}

    public static @Nullable Runnable extractRunnable(@NotNull AWTEvent event) {
        if (event instanceof InvocationEvent) {
            try {
                return (Runnable)INVOCATION_EVENT_RUNNABLE_FIELD.get(event);
            }
            catch (IllegalAccessException ignore) {
            }
        }
        return null;
    }

    public static boolean replaceRunnable(@NotNull InvocationEvent event, @NotNull Runnable newRunnable) {
        try {
            INVOCATION_EVENT_RUNNABLE_FIELD.set(event, newRunnable);
            return true;
        }
        catch (IllegalAccessException ignored) {
            return false;
        }
    }

    private static @NotNull Class<? extends Runnable> findProcessingClass() {
        try {
            return Class.forName(
                    "javax.swing.RepaintManager$ProcessingRunnable",
                    false,
                    InvocationUtil.class.getClassLoader()
            ).asSubclass(Runnable.class);
        }
        catch (ClassNotFoundException e) {
            throw new InternalAPIChangedException(RepaintManager.class, e);
        }
    }

    private static @NotNull Field findRunnableField() {
        for (Class<?> aClass = InvocationEvent.class; aClass != null; aClass = aClass.getSuperclass()) {
            try {
                Field result = aClass.getDeclaredField("runnable");
                result.setAccessible(true);
                return result;
            }
            catch (NoSuchFieldException ignore) {
            }
        }

        throw new InternalAPIChangedException(InvocationEvent.class, new NoSuchFieldException(
                "Class: " + InvocationEvent.class + " fieldName: " + "runnable" + " fieldType: " + Runnable.class));
    }

    private static final class InternalAPIChangedException extends RuntimeException {
        InternalAPIChangedException(@NotNull Class<?> targetClass,
                @Nullable ReflectiveOperationException cause) {
            super(targetClass + " class internal API has been changed", cause);
        }
    }

    public static boolean priorityEventPending() {
        ApplicationEx app = ApplicationManagerEx.getApplicationEx();
        if (app != null) {
            app.flushNativeEventQueue();
        }
        AWTEvent event = Toolkit.getDefaultToolkit().getSystemEventQueue().peekEvent();
        return event != null && event.getClass().getName().equals("sun.awt.PeerEvent");
    }
}