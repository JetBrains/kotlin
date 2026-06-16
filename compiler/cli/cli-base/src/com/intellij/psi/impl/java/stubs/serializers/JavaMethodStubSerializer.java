/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package com.intellij.psi.impl.java.stubs.serializers;

import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.PsiNameHelper;
import com.intellij.psi.PsiTypeParameterList;
import com.intellij.psi.impl.cache.ModifierFlags;
import com.intellij.psi.impl.cache.RecordUtil;
import com.intellij.psi.impl.cache.TypeInfo;
import com.intellij.psi.impl.java.stubs.*;
import com.intellij.psi.impl.java.stubs.impl.PsiMethodStubImpl;
import com.intellij.psi.impl.java.stubs.index.JavaStubIndexKeys;
import com.intellij.psi.stubs.*;
import com.intellij.psi.tree.java.IJavaElementType;
import com.intellij.psi.util.TypeConversionUtil;
import com.intellij.util.BitUtil;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class JavaMethodStubSerializer implements StubSerializer<PsiMethodStub> {
  @NotNull private final IJavaElementType myType;

  public JavaMethodStubSerializer(@NotNull IJavaElementType elementType) {
    myType = elementType;
  }

  @SuppressWarnings("UnstableApiUsage")
  @Override
  public void serialize(@NotNull PsiMethodStub stub, @NotNull StubOutputStream dataStream) throws IOException {
    dataStream.writeName(stub.getName());
    TypeInfo.writeTYPE(dataStream, stub.getReturnTypeText());
    dataStream.writeByte(((PsiMethodStubImpl)stub).getFlags());
    if (stub.isAnnotationMethod()) {
      dataStream.writeName(stub.getDefaultValueText());
    }
  }

  @SuppressWarnings("UnstableApiUsage")
  @Override
  public @NotNull PsiMethodStub deserialize(@NotNull StubInputStream dataStream, StubElement parentStub) throws IOException {
    String name = dataStream.readNameString();
    TypeInfo type = TypeInfo.readTYPE(dataStream);
    byte flags = dataStream.readByte();
    String defaultMethodValue = PsiMethodStubImpl.isAnnotationMethod(flags) ? dataStream.readNameString() : null;
    return new PsiMethodStubImpl(parentStub, name, type, flags, defaultMethodValue);
  }

  @SuppressWarnings("UnstableApiUsage")
  @Override
  public void indexStub(@NotNull PsiMethodStub stub, @NotNull IndexSink sink) {
    String name = stub.getName();
    if (name != null) {
      sink.occurrence(JavaStubIndexKeys.METHODS, name);
      if (RecordUtil.isStaticNonPrivateMember(stub)) {
        sink.occurrence(JavaStubIndexKeys.JVM_STATIC_MEMBERS_NAMES, name);
        sink.occurrence(JavaStubIndexKeys.JVM_STATIC_MEMBERS_TYPES, stub.getReturnTypeText().getShortTypeText());
      }
    }

    Set<String> methodTypeParams = getVisibleTypeParameters(stub);
    for (StubElement<?> stubElement : stub.getChildrenStubs()) {
      if (stubElement instanceof PsiParameterListStub) {
        for (StubElement<?> paramStub : stubElement.getChildrenStubs()) {
          if (paramStub instanceof PsiParameterStub) {
            TypeInfo type = ((PsiParameterStub)paramStub).getType();
            String typeName = PsiNameHelper.getShortClassName(type.text());
            if (TypeConversionUtil.isPrimitive(typeName) || TypeConversionUtil.isPrimitiveWrapper(typeName)) continue;
            if (!methodTypeParams.contains(typeName)) {
              sink.occurrence(JavaStubIndexKeys.METHOD_TYPES, typeName);
            }
          }
        }
        break;
      }
    }
  }

  @Override
  public @NotNull String getExternalId() {
    return "java." + myType;
  }
  
  private static @NotNull Set<String> getVisibleTypeParameters(@NotNull StubElement<?> stub) {
    Set<String> result = null;
    while (stub != null) {
      Set<String> names = getOwnTypeParameterNames(stub);
      if (!names.isEmpty()) {
        if (result == null) result = new HashSet<>();
        result.addAll(names);
      }

      if (isStatic(stub)) break;

      stub = stub.getParentStub();
    }
    return result == null ? Collections.emptySet() : result;
  }

  private static boolean isStatic(@NotNull StubElement<?> stub) {
    if (stub instanceof PsiMemberStub) {
        @SuppressWarnings("unchecked")
        StubElement<PsiModifierList> modList =
        (StubElement<PsiModifierList>)stub.findChildStubByElementType(JavaStubElementTypes.MODIFIER_LIST);
      if (modList != null) {
        return BitUtil.isSet(((PsiModifierListStub)modList).getModifiersMask(),
                             ModifierFlags.NAME_TO_MODIFIER_FLAG_MAP.getInt(PsiModifier.STATIC));
      }
    }
    return false;
  }

  private static Set<String> getOwnTypeParameterNames(StubElement<?> stubElement) {
    @SuppressWarnings("unchecked")
    StubElement<PsiTypeParameterList> typeParamList =
      (StubElement<PsiTypeParameterList>)stubElement.findChildStubByElementType(JavaStubElementTypes.TYPE_PARAMETER_LIST);
    if (typeParamList == null) return Collections.emptySet();

    Set<String> methodTypeParams = null;
    List<StubElement<?>> children = typeParamList.getChildrenStubs();
    for (StubElement<?> child : children) {
      if (child instanceof PsiTypeParameterStub) {
        if (methodTypeParams == null) methodTypeParams = new HashSet<>();
        methodTypeParams.add(((PsiTypeParameterStub)child).getName());
      }
    }
    return methodTypeParams == null ? Collections.emptySet() : methodTypeParams;
  }
}
