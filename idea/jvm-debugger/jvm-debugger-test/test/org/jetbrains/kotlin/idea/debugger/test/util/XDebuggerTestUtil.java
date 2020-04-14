/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.test.util;

import com.intellij.openapi.util.Pair;
import com.intellij.util.ui.TextTransferable;
import com.intellij.xdebugger.frame.XExecutionStack;
import com.intellij.xdebugger.frame.XStackFrame;
import com.intellij.xdebugger.frame.XValue;
import com.intellij.xdebugger.frame.XValueContainer;
import com.intellij.xdebugger.impl.frame.XStackFrameContainerEx;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;

import static org.junit.Assert.assertNull;

/**
 * Kotlin clone of com.intellij.xdebugger.XDebuggerTestUtil
 */
public class XDebuggerTestUtil {
    public static final int TIMEOUT_MS = 25_000;

    public static List<XStackFrame> collectFrames(@NotNull XExecutionStack thread) {
        return collectFrames(thread, TIMEOUT_MS * 2);
    }

    public static List<XStackFrame> collectFrames(XExecutionStack thread, long timeout) {
        return collectFrames(thread, timeout, XDebuggerTestUtil::waitFor);
    }

    public static List<XStackFrame> collectFrames(XExecutionStack thread, long timeout, BiFunction<Semaphore, Long, Boolean> waitFunction) {
        return collectFramesWithError(thread, timeout, waitFunction).first;
    }

    public static String getFramePresentation(XStackFrame frame) {
        TextTransferable.ColoredStringBuilder builder = new TextTransferable.ColoredStringBuilder();
        frame.customizePresentation(builder);
        return builder.getBuilder().toString();
    }

    public static Pair<List<XStackFrame>, String> collectFramesWithError(XExecutionStack thread, long timeout, BiFunction<Semaphore, Long, Boolean> waitFunction) {
        XTestStackFrameContainer container = new XTestStackFrameContainer();
        thread.computeStackFrames(0, container);
        return container.waitFor(timeout, waitFunction);
    }

    public static boolean waitFor(Semaphore semaphore, long timeoutInMillis) {
        long end = System.currentTimeMillis() + timeoutInMillis;
        long remaining = timeoutInMillis;
        do {
            try {
                return semaphore.tryAcquire(remaining, TimeUnit.MILLISECONDS);
            }
            catch (InterruptedException ignored) {
                remaining = end - System.currentTimeMillis();
            }
        } while (remaining > 0);
        return false;
    }

    public static class XTestStackFrameContainer extends XTestContainer<XStackFrame> implements XStackFrameContainerEx {
        public volatile XStackFrame frameToSelect;

        @Override
        public void addStackFrames(@NotNull List<? extends XStackFrame> stackFrames, boolean last) {
            addChildren(stackFrames, last);
        }

        @Override
        public void addStackFrames(@NotNull List<? extends XStackFrame> stackFrames, @Nullable XStackFrame toSelect, boolean last) {
            if (toSelect != null) frameToSelect = toSelect;
            addChildren(stackFrames, last);
        }

        @Override
        public void errorOccurred(@NotNull String errorMessage) {
            setErrorMessage(errorMessage);
        }
    }

    @NotNull
    public static List<XValue> collectChildren(XValueContainer value) {
        return collectChildren(value, XDebuggerTestUtil::waitFor);
    }

    @NotNull
    public static List<XValue> collectChildren(XValueContainer value, BiFunction<Semaphore, Long, Boolean> waitFunction) {
        final Pair<List<XValue>, String> childrenWithError = collectChildrenWithError(value, waitFunction);
        final String error = childrenWithError.second;
        assertNull("Error getting children: " + error, error);
        return childrenWithError.first;
    }

    @NotNull
    public static Pair<List<XValue>, String> collectChildrenWithError(XValueContainer value) {
        return collectChildrenWithError(value, XDebuggerTestUtil::waitFor);
    }

    @NotNull
    public static Pair<List<XValue>, String> collectChildrenWithError(XValueContainer value,
            BiFunction<Semaphore, Long, Boolean> waitFunction) {
        XTestCompositeNode container = new XTestCompositeNode();
        value.computeChildren(container);

        return container.waitFor(TIMEOUT_MS, waitFunction);
    }
}
