/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.psi.stubs.KotlinBackingFieldStub;
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes;

public class KtBackingField extends KtDeclarationStub<KotlinBackingFieldStub>
        implements KtModifierListOwner, KtDeclarationWithInitializer {
    public KtBackingField(@NotNull ASTNode node) {
        super(node);
    }

    public KtBackingField(@NotNull KotlinBackingFieldStub stub) {
        super(stub, KtStubElementTypes.BACKING_FIELD);
    }

    @Override
    public <R, D> R accept(@NotNull KtVisitor<R, D> visitor, D data) {
        return visitor.visitBackingField(this, data);
    }

    @Nullable
    public PsiElement getEqualsToken() {
        return findChildByType(KtTokens.EQ);
    }

    @Nullable
    public KtTypeReference getReturnTypeReference() {
        return getStubOrPsiChild(KtStubElementTypes.TYPE_REFERENCE);
    }

    @NotNull
    public PsiElement getNamePlaceholder() {
        PsiElement it = getFieldKeyword();
        if (it != null) {
            return it;
        }
        return getNode().getPsi();
    }

    @Nullable
    @Override
    public KtExpression getInitializer() {
        KotlinBackingFieldStub stub = getGreenStub();
        if (stub != null && !stub.hasInitializer()) {
            return null;
        }
        return PsiTreeUtil.getNextSiblingOfType(getEqualsToken(), KtExpression.class);
    }

    @Override
    public boolean hasInitializer() {
        KotlinBackingFieldStub stub = getGreenStub();
        if (stub != null) {
            return stub.hasInitializer();
        }
        return getInitializer() != null;
    }

    @Override
    public int getTextOffset() {
        return getNamePlaceholder().getTextRange().getStartOffset();
    }

    public PsiElement getFieldKeyword() {
        return findChildByType(KtTokens.FIELD_KEYWORD);
    }
}
