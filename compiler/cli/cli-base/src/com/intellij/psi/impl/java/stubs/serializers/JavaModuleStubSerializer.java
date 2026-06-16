/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package com.intellij.psi.impl.java.stubs.serializers;

import com.intellij.psi.impl.java.stubs.impl.PsiJavaModuleStubImpl;
import com.intellij.psi.impl.java.stubs.index.JavaStubIndexKeys;
import com.intellij.psi.stubs.*;
import com.intellij.psi.tree.java.IJavaElementType;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class JavaModuleStubSerializer implements StubSerializer<PsiJavaModuleStubImpl> {
  @NotNull private final IJavaElementType myType;

  public JavaModuleStubSerializer(@NotNull IJavaElementType elementType) {
    myType = elementType;
  }

  @Override
  public void serialize(@NotNull PsiJavaModuleStubImpl stub, @NotNull StubOutputStream dataStream) throws IOException {
    dataStream.writeName(stub.getName());
    dataStream.writeVarInt(stub.getResolution());
  }

  @Override
  public @NotNull PsiJavaModuleStubImpl deserialize(@NotNull StubInputStream dataStream, StubElement parentStub) throws IOException {
    return new PsiJavaModuleStubImpl(parentStub, dataStream.readNameString(), dataStream.readVarInt());
  }

  @Override
  public void indexStub(@NotNull PsiJavaModuleStubImpl stub, @NotNull IndexSink sink) {
    sink.occurrence(JavaStubIndexKeys.MODULE_NAMES, stub.getName());
  }

  @Override
  public @NotNull String getExternalId() {
    return "java." + myType;
  }
}
