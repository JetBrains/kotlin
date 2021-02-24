/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.test.util;

import com.intellij.openapi.util.Pair;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.util.SmartList;
import com.intellij.xdebugger.frame.XDebuggerTreeNodeHyperlink;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.function.BiFunction;

public class XTestContainer<T> {
  private final List<T> myChildren = new SmartList<>();
  private String myErrorMessage;
  private final Semaphore myFinished = new Semaphore(0);

  public void addChildren(List<? extends T> children, boolean last) {
    myChildren.addAll(children);
    if (last) myFinished.release();
  }

  public void tooManyChildren(int remaining) {
    myFinished.release();
  }

  public void setMessage(@NotNull String message, Icon icon, @NotNull final SimpleTextAttributes attributes, @Nullable XDebuggerTreeNodeHyperlink link) {
  }

  public void setErrorMessage(@NotNull String message, @Nullable XDebuggerTreeNodeHyperlink link) {
    setErrorMessage(message);
  }

  public void setErrorMessage(@NotNull String errorMessage) {
    myErrorMessage = errorMessage;
    myFinished.release();
  }

  @NotNull
  public Pair<List<T>, String> waitFor(long timeoutMs) {
    return waitFor(timeoutMs, (semaphore, timeout) -> XDebuggerTestUtil.waitFor(myFinished, timeout));
  }

  @NotNull
  public Pair<List<T>, String> waitFor(long timeoutMs, BiFunction<? super Semaphore, ? super Long, Boolean> waitFunction) {
    if (!waitFunction.apply(myFinished, timeoutMs)) {
      throw new AssertionError("Waiting timed out");
    }

    return Pair.create(myChildren, myErrorMessage);
  }
}
