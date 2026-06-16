/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package com.intellij.psi.impl.java.stubs.serializers;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiNameHelper;
import com.intellij.psi.impl.cache.TypeInfo;
import com.intellij.psi.impl.java.stubs.JavaClassElementType;
import com.intellij.psi.impl.java.stubs.PsiClassStub;
import com.intellij.psi.impl.java.stubs.factories.JavaClassStubFactory;
import com.intellij.psi.impl.java.stubs.impl.PsiClassStubImpl;
import com.intellij.psi.impl.java.stubs.index.JavaStubIndexKeys;
import com.intellij.psi.stubs.*;
import com.intellij.psi.tree.java.IJavaElementType;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class JavaClassStubSerializer implements StubSerializer<PsiClassStub<PsiClass>> {
  @NotNull private final IJavaElementType myType;

  public JavaClassStubSerializer(@NotNull IJavaElementType elementType) {
    myType = elementType;
  }

  @SuppressWarnings("UnstableApiUsage")
  @Override
  public void serialize(@NotNull PsiClassStub<PsiClass> stub, @NotNull StubOutputStream dataStream) throws IOException {
    dataStream.writeShort(((PsiClassStubImpl<?>)stub).getFlags());
    if (!stub.isAnonymous()) {
      String name = stub.getName();
      TypeInfo info = ((PsiClassStubImpl<?>)stub).getQualifiedNameTypeInfo();
      dataStream.writeName(info.getShortTypeText().equals(name) ? null : name);
      TypeInfo.writeTYPE(dataStream, info);
      dataStream.writeName(stub.getSourceFileName());
    }
    else {
      dataStream.writeName(stub.getBaseClassReferenceText());
    }
  }

  @SuppressWarnings("UnstableApiUsage")
  @Override
  public @NotNull PsiClassStub<PsiClass> deserialize(@NotNull StubInputStream dataStream, StubElement parentStub) throws IOException {
    short flags = dataStream.readShort();
    boolean isAnonymous = PsiClassStubImpl.isAnonymous(flags);
    boolean isEnumConst = PsiClassStubImpl.isEnumConstInitializer(flags);
    boolean isImplicit = PsiClassStubImpl.isImplicit(flags);
    JavaClassElementType type = JavaClassStubFactory.typeForClass(isAnonymous, isEnumConst, isImplicit);

    if (!isAnonymous) {
      String name = dataStream.readNameString();
      TypeInfo typeInfo = TypeInfo.readTYPE(dataStream);
      if (name == null) {
        name = typeInfo.getShortTypeText();
      }
      String sourceFileName = dataStream.readNameString();
      PsiClassStubImpl<PsiClass> classStub = new PsiClassStubImpl<>(type, parentStub, typeInfo, name, null, flags);
      classStub.setSourceFileName(sourceFileName);
      return classStub;
    }
    else {
      String baseRef = dataStream.readNameString();
      return new PsiClassStubImpl<>(type, parentStub, TypeInfo.SimpleTypeInfo.NULL, null, baseRef, flags);
    }
  }

  @SuppressWarnings("UnstableApiUsage")
  @Override
  public void indexStub(@NotNull PsiClassStub<PsiClass> stub, @NotNull IndexSink sink) {
    if (stub.isImplicit()) {
      String name = stub.getName();
      if (name != null) {
        sink.occurrence(JavaStubIndexKeys.IMPLICIT_CLASSES, name);
      }
      return;
    }
    boolean isAnonymous = stub.isAnonymous();
    if (isAnonymous) {
      String baseRef = stub.getBaseClassReferenceText();
      if (baseRef != null) {
        sink.occurrence(JavaStubIndexKeys.ANONYMOUS_BASEREF, PsiNameHelper.getShortClassName(baseRef));
      }
    }
    else {
      String shortName = stub.getName();
      if (shortName != null && (!(stub instanceof PsiClassStubImpl) || !((PsiClassStubImpl<?>)stub).isAnonymousInner())) {
        sink.occurrence(JavaStubIndexKeys.CLASS_SHORT_NAMES, shortName);
      }

      String fqn = stub.getQualifiedName();
      if (fqn != null) {
        sink.occurrence(JavaStubIndexKeys.CLASS_FQN, fqn);
      }
    }
  }

  @Override
  public @NotNull String getExternalId() {
    return "java." + myType;
  }
}
