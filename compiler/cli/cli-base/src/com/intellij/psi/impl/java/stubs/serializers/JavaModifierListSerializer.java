/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package com.intellij.psi.impl.java.stubs.serializers;

import com.intellij.psi.impl.java.stubs.impl.PsiModifierListStubImpl;
import com.intellij.psi.stubs.*;
import com.intellij.psi.tree.java.IJavaElementType;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class JavaModifierListSerializer implements StubSerializer<PsiModifierListStubImpl> {
  @NotNull private final IJavaElementType myType;

  public JavaModifierListSerializer(@NotNull IJavaElementType elementType) {
    myType = elementType;
  }

  @Override
  public void serialize(@NotNull PsiModifierListStubImpl stub, @NotNull StubOutputStream dataStream) throws IOException {
    dataStream.writeVarInt(stub.getModifiersMask());
  }

  @Override
  public @NotNull PsiModifierListStubImpl deserialize(@NotNull StubInputStream dataStream, StubElement parentStub) throws IOException {
    return new PsiModifierListStubImpl(parentStub, dataStream.readVarInt());
  }

  @Override
  public void indexStub(@NotNull PsiModifierListStubImpl stub, @NotNull IndexSink sink) { }

  @Override
  public @NotNull String getExternalId() {
    return "java." + myType;
  }
}
