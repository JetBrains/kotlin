/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package com.intellij.psi.impl.java.stubs.serializers;

import com.intellij.psi.impl.java.stubs.impl.PsiImportListStubImpl;
import com.intellij.psi.stubs.*;
import com.intellij.psi.tree.java.IJavaElementType;
import org.jetbrains.annotations.NotNull;

public class JavaImportListStubSerializer implements StubSerializer<PsiImportListStubImpl> {
  @NotNull private final IJavaElementType myType;

  public JavaImportListStubSerializer(@NotNull IJavaElementType elementType) {
    myType = elementType;
  }

  @Override
  public void serialize(@NotNull PsiImportListStubImpl stub, @NotNull StubOutputStream dataStream) {
  }

  @Override
  public @NotNull PsiImportListStubImpl deserialize(@NotNull StubInputStream dataStream, StubElement parentStub) {
    return new PsiImportListStubImpl(parentStub);
  }

  @Override
  public void indexStub(@NotNull PsiImportListStubImpl stub, @NotNull IndexSink sink) { }

  @Override
  public @NotNull String getExternalId() {
    return "java." + myType;
  }
}
