/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package com.intellij.psi.impl.java.stubs.serializers;

import com.intellij.psi.impl.java.stubs.impl.PsiNameValuePairStubImpl;
import com.intellij.psi.stubs.*;
import com.intellij.psi.tree.java.IJavaElementType;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class JavaNameValuePairStubSerializer implements StubSerializer<PsiNameValuePairStubImpl> {
  @NotNull private final IJavaElementType myType;

  public JavaNameValuePairStubSerializer(@NotNull IJavaElementType elementType) {
    myType = elementType;
  }

  @Override
  public void serialize(@NotNull PsiNameValuePairStubImpl stub, @NotNull StubOutputStream dataStream) throws IOException {
    dataStream.writeName(stub.getName());

    String value = stub.getValue();
    boolean hasValue = value != null;
    dataStream.writeBoolean(hasValue);
    if (hasValue) {
      dataStream.writeUTFFast(value);
    }
  }

  @Override
  public @NotNull PsiNameValuePairStubImpl deserialize(@NotNull StubInputStream dataStream, StubElement parentStub) throws IOException {
    String name = dataStream.readNameString();
    boolean hasValue = dataStream.readBoolean();
    return new PsiNameValuePairStubImpl(parentStub, name, hasValue ? dataStream.readUTFFast() : null);
  }

  @Override
  public void indexStub(@NotNull PsiNameValuePairStubImpl stub, @NotNull IndexSink sink) { }

  @Override
  public @NotNull String getExternalId() {
    return "java." + myType;
  }
}
