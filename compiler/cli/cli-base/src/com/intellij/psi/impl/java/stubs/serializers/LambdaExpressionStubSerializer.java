/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package com.intellij.psi.impl.java.stubs.serializers;

import com.intellij.psi.PsiLambdaExpression;
import com.intellij.psi.impl.java.stubs.FunctionalExpressionStub;
import com.intellij.psi.stubs.*;
import com.intellij.psi.tree.java.IJavaElementType;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class LambdaExpressionStubSerializer implements StubSerializer<FunctionalExpressionStub<PsiLambdaExpression>> {
  @NotNull private final IJavaElementType myType;

  public LambdaExpressionStubSerializer(@NotNull IJavaElementType elementType) {
    myType = elementType;
  }

  @Override
  public void serialize(@NotNull FunctionalExpressionStub<PsiLambdaExpression> stub, @NotNull StubOutputStream dataStream) throws IOException {
    dataStream.writeUTFFast(stub.getPresentableText());
  }

  @Override
  public @NotNull FunctionalExpressionStub<PsiLambdaExpression> deserialize(@NotNull StubInputStream dataStream, StubElement parentStub) throws IOException {
    return new FunctionalExpressionStub<>(parentStub, myType, dataStream.readUTFFast());
  }

  @Override
  public void indexStub(@NotNull FunctionalExpressionStub<PsiLambdaExpression> stub, @NotNull IndexSink sink) { }

  @Override
  public @NotNull String getExternalId() {
    return "java." + myType;
  }
}
