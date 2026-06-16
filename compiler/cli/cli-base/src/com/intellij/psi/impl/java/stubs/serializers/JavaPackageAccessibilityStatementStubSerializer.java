/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package com.intellij.psi.impl.java.stubs.serializers;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.impl.java.stubs.impl.PsiPackageAccessibilityStatementStubImpl;
import com.intellij.psi.stubs.*;
import com.intellij.psi.tree.java.IJavaElementType;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;

public class JavaPackageAccessibilityStatementStubSerializer implements StubSerializer<PsiPackageAccessibilityStatementStubImpl> {
  @NotNull private final IJavaElementType myType;

  public JavaPackageAccessibilityStatementStubSerializer(@NotNull IJavaElementType elementType) {
    myType = elementType;
  }

  @Override
  public void serialize(@NotNull PsiPackageAccessibilityStatementStubImpl stub, @NotNull StubOutputStream dataStream) throws IOException {
    dataStream.writeName(stub.getPackageName());
    dataStream.writeUTFFast(StringUtil.join(stub.getTargets(), "/"));
  }

  @Override
  public @NotNull PsiPackageAccessibilityStatementStubImpl deserialize(@NotNull StubInputStream dataStream, StubElement parentStub) throws IOException {
    String packageName = dataStream.readNameString();
    List<String> targets = StringUtil.split(dataStream.readUTFFast(), "/");
    return new PsiPackageAccessibilityStatementStubImpl(parentStub, myType, packageName, targets);
  }

  @Override
  public void indexStub(@NotNull PsiPackageAccessibilityStatementStubImpl stub, @NotNull IndexSink sink) { }

  @Override
  public @NotNull String getExternalId() {
    return "java." + myType;
  }
}
