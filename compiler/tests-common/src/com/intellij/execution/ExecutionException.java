/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.intellij.execution;

import com.intellij.openapi.util.NlsContexts;

import java.io.IOException;

public class ExecutionException extends Exception {
  public ExecutionException(final @NlsContexts.DialogMessage String s) {
    super(s);
  }

  public ExecutionException(final Throwable cause) {
    super(cause == null ? null : cause.getMessage(), cause);
  }

  public ExecutionException(final @NlsContexts.DialogMessage String s, Throwable cause) {
    super(s, cause);
  }

  public IOException toIOException() {
    final Throwable cause = getCause();
    return cause instanceof IOException ? (IOException)cause : new IOException(this);
  }
}
