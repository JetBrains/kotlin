// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.openapi.fileTypes;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.util.KeyedLazyInstance;

/**
 * @see BinaryFileDecompiler
 * TODO: Remove when figure something out via KT-81715
 */
@Service
public final class BinaryFileTypeDecompilers extends FileTypeExtension<BinaryFileDecompiler> {
  private static final ExtensionPointName<KeyedLazyInstance<BinaryFileDecompiler>> EP_NAME =
    new ExtensionPointName<>("com.intellij.filetype.decompiler");

  private BinaryFileTypeDecompilers() {
    super(EP_NAME);
  }

  public void notifyDecompilerSetChange() {
    // We do nothing here because the decompiler set is not supposed to change during the lifetime of the compiler.
    // But hopefully, we'll find some other way to fix the issue via KT-81715
  }

  public static BinaryFileTypeDecompilers getInstance() {
    return ApplicationManager.getApplication().getService(BinaryFileTypeDecompilers.class);
  }
}