/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package com.intellij.psi.impl.java.stubs.serializers;

import com.intellij.psi.impl.cache.TypeInfo;
import com.intellij.psi.impl.java.stubs.impl.PsiRecordComponentStubImpl;
import com.intellij.psi.impl.java.stubs.index.JavaStubIndexKeys;
import com.intellij.psi.stubs.*;
import com.intellij.psi.tree.java.IJavaElementType;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class JavaRecordComponentStubSerializer implements StubSerializer<PsiRecordComponentStubImpl> {
  @NotNull private final IJavaElementType myType;

  public JavaRecordComponentStubSerializer(@NotNull IJavaElementType elementType) {
    myType = elementType;
  }

  @SuppressWarnings("UnstableApiUsage")
  @Override
  public void serialize(@NotNull PsiRecordComponentStubImpl stub, @NotNull StubOutputStream dataStream) throws IOException {
    dataStream.writeName(stub.getName());
    TypeInfo.writeTYPE(dataStream, stub.getType());
    dataStream.writeByte(stub.getFlags());
  }

  @SuppressWarnings("UnstableApiUsage")
  @Override
  public @NotNull PsiRecordComponentStubImpl deserialize(@NotNull StubInputStream dataStream, StubElement parentStub) throws IOException {
    String name = dataStream.readNameString();
    TypeInfo type = TypeInfo.readTYPE(dataStream);
    byte flags = dataStream.readByte();
    return new PsiRecordComponentStubImpl(parentStub, name, type, flags);
  }

  @Override
  public void indexStub(@NotNull PsiRecordComponentStubImpl stub, @NotNull IndexSink sink) {
    String name = stub.getName();
    sink.occurrence(JavaStubIndexKeys.RECORD_COMPONENTS, name);
  }

  @Override
  public @NotNull String getExternalId() {
    return "java." + myType;
  }
}
