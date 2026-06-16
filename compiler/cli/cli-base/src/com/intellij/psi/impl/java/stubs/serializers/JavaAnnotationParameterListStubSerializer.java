/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package com.intellij.psi.impl.java.stubs.serializers;

import com.intellij.psi.impl.java.stubs.impl.PsiAnnotationParameterListStubImpl;
import com.intellij.psi.stubs.*;
import com.intellij.psi.tree.java.IJavaElementType;
import org.jetbrains.annotations.NotNull;

public class JavaAnnotationParameterListStubSerializer implements StubSerializer<PsiAnnotationParameterListStubImpl> {
  @NotNull private final IJavaElementType myType;

  public JavaAnnotationParameterListStubSerializer(@NotNull IJavaElementType elementType) {
    myType = elementType;
  }

  @Override
  public void serialize(@NotNull PsiAnnotationParameterListStubImpl stub, @NotNull StubOutputStream dataStream) {

  }

  @Override
  public @NotNull PsiAnnotationParameterListStubImpl deserialize(@NotNull StubInputStream dataStream, StubElement parentStub) {
    return new PsiAnnotationParameterListStubImpl(parentStub);
  }

  @Override
  public void indexStub(@NotNull PsiAnnotationParameterListStubImpl stub, @NotNull IndexSink sink) {

  }

  @Override
  public @NotNull String getExternalId() {
    return "java." + myType;
  }
}
