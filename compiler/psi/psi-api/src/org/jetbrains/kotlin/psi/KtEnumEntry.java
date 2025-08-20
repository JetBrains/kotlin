/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.KtStubBasedElementTypes;
import org.jetbrains.kotlin.name.ClassId;
import org.jetbrains.kotlin.psi.psiUtil.KtPsiUtilKt;
import org.jetbrains.kotlin.psi.stubs.KotlinClassStub;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class KtEnumEntry extends KtClass implements KtDeclarationWithReturnType {
    public KtEnumEntry(@NotNull ASTNode node) {
        super(node);
    }

    public KtEnumEntry(@NotNull KotlinClassStub stub) {
        super(stub, KtStubBasedElementTypes.ENUM_ENTRY);
    }

    @NotNull
    @Override
    public List<KtSuperTypeListEntry> getSuperTypeListEntries() {
        KtInitializerList initializerList = getInitializerList();
        if (initializerList == null) {
            return Collections.emptyList();
        }
        return initializerList.getInitializers();
    }

    public boolean hasInitializer() {
        return !getSuperTypeListEntries().isEmpty();
    }

    @Nullable
    @Override
    public ClassId getClassId() {
        return null;
    }

    @Nullable
    @SuppressWarnings("deprecation")
    public KtInitializerList getInitializerList() {
        return getStubOrPsiChild(KtStubBasedElementTypes.INITIALIZER_LIST);
    }

    @Override
    public boolean isEquivalentTo(@Nullable PsiElement another) {
        if (this == another) return true;
        if (!(another instanceof KtEnumEntry)) return false;
        KtEnumEntry anotherEnumEntry = (KtEnumEntry) another;

        if (!Objects.equals(getName(), anotherEnumEntry.getName())) return false;

        KtClassOrObject thisContainingClass = KtPsiUtilKt.getContainingClassOrObject(this);
        if (thisContainingClass == null) return false;

        KtClassOrObject anotherContainingClass = KtPsiUtilKt.getContainingClassOrObject(anotherEnumEntry);
        return thisContainingClass.isEquivalentTo(anotherContainingClass);
    }

    @Override
    public <R, D> R accept(@NotNull KtVisitor<R, D> visitor, D data) {
        return visitor.visitEnumEntry(this, data);
    }

    @Nullable
    @Override
    public KtTypeReference getTypeReference() {
        return null;
    }
}
