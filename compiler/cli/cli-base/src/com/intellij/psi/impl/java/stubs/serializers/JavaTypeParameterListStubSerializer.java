/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package com.intellij.psi.impl.java.stubs.serializers;

import com.intellij.psi.impl.java.stubs.impl.PsiTypeParameterListStubImpl;
import com.intellij.psi.stubs.*;
import com.intellij.psi.tree.java.IJavaElementType;
import org.jetbrains.annotations.NotNull;

public class JavaTypeParameterListStubSerializer implements StubSerializer<PsiTypeParameterListStubImpl> {
  @NotNull private final IJavaElementType myType;

  public JavaTypeParameterListStubSerializer(@NotNull IJavaElementType elementType) {
    myType = elementType;
  }

  @Override
  public void serialize(@NotNull PsiTypeParameterListStubImpl stub, @NotNull StubOutputStream dataStream) {
  }

  @Override
  public @NotNull PsiTypeParameterListStubImpl deserialize(@NotNull StubInputStream dataStream, StubElement parentStub) {
    return new PsiTypeParameterListStubImpl(parentStub);
  }

  @Override
  public void indexStub(@NotNull PsiTypeParameterListStubImpl stub, @NotNull IndexSink sink) { }

  @Override
  public @NotNull String getExternalId() {
    return "java." + myType;
  }
}
