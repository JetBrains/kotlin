/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package com.intellij.psi.impl.java.stubs.serializers;

import com.intellij.psi.impl.java.stubs.impl.PsiClassInitializerStubImpl;
import com.intellij.psi.stubs.*;
import com.intellij.psi.tree.java.IJavaElementType;
import org.jetbrains.annotations.NotNull;

// This file was hijacked from com.jetbrains.intellij.java:java-indexing-impl, as it's compiled against JDK 21
// and we cannot depend on it. See also the following issues:
// - https://youtrack.jetbrains.com/issue/OSIP-935
// - https://youtrack.jetbrains.com/issue/IJPL-248265
// Shortly, when Kotlin is compiled against JDK 21, we may drop this file.
// Or, when this class is out from java-indexing-impl and compiled against JDK 1.8, we may drop this file.

public class JavaClassInitializerStubSerializer implements StubSerializer<PsiClassInitializerStubImpl> {
  @NotNull private final IJavaElementType myType;

  public JavaClassInitializerStubSerializer(@NotNull IJavaElementType elementType) {
    myType = elementType;
  }

  @Override
  public void serialize(@NotNull PsiClassInitializerStubImpl stub, @NotNull StubOutputStream dataStream) {
    // Empty stub - nothing to serialize
  }

  @Override
  public @NotNull PsiClassInitializerStubImpl deserialize(@NotNull StubInputStream dataStream, StubElement parentStub) {
    return new PsiClassInitializerStubImpl(parentStub);
  }

  @Override
  public void indexStub(@NotNull PsiClassInitializerStubImpl stub, @NotNull IndexSink sink) { }

  @Override
  public @NotNull String getExternalId() {
    return "java." + myType;
  }
}
