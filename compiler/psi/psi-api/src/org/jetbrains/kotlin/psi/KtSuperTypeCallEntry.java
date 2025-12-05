/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.KtStubBasedElementTypes;
import org.jetbrains.kotlin.psi.stubs.KotlinPlaceHolderStub;

import java.util.Collections;
import java.util.List;

public class KtSuperTypeCallEntry extends KtSuperTypeListEntry implements KtCallElement {
    public KtSuperTypeCallEntry(@NotNull ASTNode node) {
        super(node);
    }

    public KtSuperTypeCallEntry(@NotNull KotlinPlaceHolderStub<? extends KtSuperTypeListEntry> stub) {
        super(stub, KtStubBasedElementTypes.SUPER_TYPE_CALL_ENTRY);
    }

    @Override
    public <R, D> R accept(@NotNull KtVisitor<R, D> visitor, D data) {
        return visitor.visitSuperTypeCallEntry(this, data);
    }

    @NotNull
    @Override
    @SuppressWarnings("deprecation") // KT-78356
    public KtConstructorCalleeExpression getCalleeExpression() {
        return getRequiredStubOrPsiChild(KtStubBasedElementTypes.CONSTRUCTOR_CALLEE);
    }

    @Override
    @Nullable
    @SuppressWarnings("deprecation") // KT-78356
    public KtValueArgumentList getValueArgumentList() {
        return getStubOrPsiChild(KtStubBasedElementTypes.VALUE_ARGUMENT_LIST);
    }

    @Override
    @NotNull
    public List<? extends ValueArgument> getValueArguments() {
        KtValueArgumentList list = getValueArgumentList();
        return list != null ? list.getArguments() : Collections.<KtValueArgument>emptyList();
    }

    @NotNull
    @Override
    public List<KtLambdaArgument> getLambdaArguments() {
        return Collections.emptyList();
    }

    @Override
    public KtTypeReference getTypeReference() {
        return getCalleeExpression().getTypeReference();
    }

    @NotNull
    @Override
    public List<KtTypeProjection> getTypeArguments() {
        KtTypeArgumentList typeArgumentList = getTypeArgumentList();
        if (typeArgumentList == null) {
            return Collections.emptyList();
        }
        return typeArgumentList.getArguments();
    }

    @Override
    public KtTypeArgumentList getTypeArgumentList() {
        KtUserType userType = getTypeAsUserType();
        return userType != null ? userType.getTypeArgumentList() : null;
    }
}
