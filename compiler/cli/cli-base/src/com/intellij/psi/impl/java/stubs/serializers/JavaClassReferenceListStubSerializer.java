/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package com.intellij.psi.impl.java.stubs.serializers;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiNameHelper;
import com.intellij.psi.PsiReferenceList;
import com.intellij.psi.impl.cache.TypeInfo;
import com.intellij.psi.impl.java.stubs.PsiClassReferenceListStub;
import com.intellij.psi.impl.java.stubs.PsiClassStub;
import com.intellij.psi.impl.java.stubs.PsiJavaFileStub;
import com.intellij.psi.impl.java.stubs.impl.PsiClassReferenceListStubImpl;
import com.intellij.psi.impl.java.stubs.index.JavaStubIndexKeys;
import com.intellij.psi.stubs.*;
import com.intellij.psi.tree.java.IJavaElementType;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class JavaClassReferenceListStubSerializer implements StubSerializer<PsiClassReferenceListStub> {
  @NotNull private final IJavaElementType myType;

  public JavaClassReferenceListStubSerializer(@NotNull IJavaElementType elementType) {
    myType = elementType;
  }

  @SuppressWarnings("UnstableApiUsage")
  @Override
  public void serialize(@NotNull PsiClassReferenceListStub stub, @NotNull StubOutputStream dataStream) throws IOException {
    TypeInfo[] types = stub.getTypes();
    dataStream.writeVarInt(types.length);
    for (TypeInfo info : types) {
      TypeInfo.writeTYPE(dataStream, info);
    }
  }

  @SuppressWarnings("UnstableApiUsage")
  @Override
  public @NotNull PsiClassReferenceListStub deserialize(@NotNull StubInputStream dataStream, StubElement parentStub) throws IOException {
    int len = dataStream.readVarInt();
    TypeInfo[] infos = len == 0 ? TypeInfo.EMPTY_ARRAY : new TypeInfo[len];
    for (int i = 0; i < infos.length; i++) {
      infos[i] = TypeInfo.readTYPE(dataStream);
    }
    return new PsiClassReferenceListStubImpl(myType, parentStub, infos);
  }

  @Override
  public void indexStub(@NotNull PsiClassReferenceListStub stub, @NotNull IndexSink sink) {
    PsiReferenceList.Role role = stub.getRole();
    if (role == PsiReferenceList.Role.EXTENDS_LIST || role == PsiReferenceList.Role.IMPLEMENTS_LIST) {
      String[] names = stub.getReferencedNames();
      for (String name : names) {
        String shortName = PsiNameHelper.getShortClassName(name);
        if (!StringUtil.isEmptyOrSpaces(shortName)) {
          sink.occurrence(JavaStubIndexKeys.SUPER_CLASSES, shortName);
        }
      }

      if (role == PsiReferenceList.Role.EXTENDS_LIST) {
        StubElement<?> parentStub = stub.getParentStub();
        if (parentStub instanceof PsiClassStub) {
          PsiClassStub<?> psiClassStub = (PsiClassStub<?>)parentStub;
          if (psiClassStub.isEnum()) {
            sink.occurrence(JavaStubIndexKeys.SUPER_CLASSES, "Enum");
          }
          if (psiClassStub.isAnnotationType()) {
            sink.occurrence(JavaStubIndexKeys.SUPER_CLASSES, "Annotation");
          }
        }
      }
    }
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
