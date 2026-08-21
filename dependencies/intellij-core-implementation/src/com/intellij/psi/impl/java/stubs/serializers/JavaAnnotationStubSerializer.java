/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package com.intellij.psi.impl.java.stubs.serializers;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiNameHelper;
import com.intellij.psi.impl.java.stubs.PsiJavaFileStub;
import com.intellij.psi.impl.java.stubs.impl.PsiAnnotationStubImpl;
import com.intellij.psi.impl.java.stubs.index.JavaStubIndexKeys;
import com.intellij.psi.stubs.*;
import com.intellij.psi.tree.java.IJavaElementType;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

// This file was hijacked from com.jetbrains.intellij.java:java-indexing-impl, as it's compiled against JDK 21
// and we cannot depend on it. See also the following issues:
// - https://youtrack.jetbrains.com/issue/OSIP-935
// - https://youtrack.jetbrains.com/issue/IJPL-248265
// Shortly, when Kotlin is compiled against JDK 21, we may drop this file.
// Or, when this class is out from java-indexing-impl and compiled against JDK 1.8, we may drop this file.

public class JavaAnnotationStubSerializer implements StubSerializer<PsiAnnotationStubImpl> {
  @NotNull private final IJavaElementType myType;

  public JavaAnnotationStubSerializer(@NotNull IJavaElementType elementType) {
    myType = elementType;
  }

  @Override
  public void serialize(@NotNull PsiAnnotationStubImpl stub, @NotNull StubOutputStream dataStream) throws IOException {
    dataStream.writeUTFFast(stub.getText());
  }

  @Override
  public @NotNull PsiAnnotationStubImpl deserialize(@NotNull StubInputStream dataStream, StubElement parentStub) throws IOException {
    return new PsiAnnotationStubImpl(parentStub, dataStream.readUTFFast());
  }

  @Override
  public void indexStub(@NotNull PsiAnnotationStubImpl stub, @NotNull IndexSink sink) {
    String shortName = getReferenceShortName(stub.getText());
    if (!StringUtil.isEmptyOrSpaces(shortName)) {
      sink.occurrence(JavaStubIndexKeys.ANNOTATIONS, shortName);
    }
  }

  private static String getReferenceShortName(String annotationText) {
    int index = annotationText.indexOf('(');
    if (index >= 0) annotationText = annotationText.substring(0, index);
    return PsiNameHelper.getShortClassName(annotationText);
  }

  @Override
  public boolean isAlwaysLeaf(@NotNull StubBase<?> root) {
    return root instanceof PsiJavaFileStub && ((PsiJavaFileStub)root).isCompiled();
  }

  @Override
  public @NotNull String getExternalId() {
    return "java." + myType;
  }
}
