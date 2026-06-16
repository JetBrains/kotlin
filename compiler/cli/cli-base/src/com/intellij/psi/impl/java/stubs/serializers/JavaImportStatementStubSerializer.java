/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package com.intellij.psi.impl.java.stubs.serializers;

import com.intellij.psi.impl.java.stubs.PsiImportStatementStub;
import com.intellij.psi.impl.java.stubs.impl.PsiImportStatementStubImpl;
import com.intellij.psi.stubs.*;
import com.intellij.psi.tree.java.IJavaElementType;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class JavaImportStatementStubSerializer implements StubSerializer<PsiImportStatementStub> {
  @NotNull private final IJavaElementType myType;

  public JavaImportStatementStubSerializer(@NotNull IJavaElementType elementType) {
    myType = elementType;
  }

  @Override
  public void serialize(@NotNull PsiImportStatementStub stub, @NotNull StubOutputStream dataStream) throws IOException {
    dataStream.writeByte(((PsiImportStatementStubImpl)stub).getFlags());
    dataStream.writeName(stub.getImportReferenceText());
  }

  @Override
  public @NotNull PsiImportStatementStub deserialize(@NotNull StubInputStream dataStream, StubElement parentStub) throws IOException {
    byte flags = dataStream.readByte();
    String refText = dataStream.readNameString();
    return new PsiImportStatementStubImpl(parentStub, refText, flags);
  }

  @Override
  public void indexStub(@NotNull PsiImportStatementStub stub, @NotNull IndexSink sink) {
  }

  @Override
  public @NotNull String getExternalId() {
    return "java." + myType;
  }
}
