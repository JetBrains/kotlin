/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.KtStubBasedElementTypes;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.psi.stubs.KotlinTypeProjectionStub;

public class KtTypeProjection extends KtModifierListOwnerStub<KotlinTypeProjectionStub> {

    public KtTypeProjection(@NotNull ASTNode node) {
        super(node);
    }

    public KtTypeProjection(@NotNull KotlinTypeProjectionStub stub) {
        super(stub, KtStubBasedElementTypes.TYPE_PROJECTION);
    }

    @NotNull
    public KtProjectionKind getProjectionKind() {
        KotlinTypeProjectionStub stub = getGreenStub();
        if (stub != null) {
            return stub.getProjectionKind();
        }

        PsiElement projectionToken = getProjectionToken();
        IElementType token = projectionToken != null ? projectionToken.getNode().getElementType() : null;
        for (KtProjectionKind projectionKind : KtProjectionKind.values()) {
            if (projectionKind.getToken() == token) {
                return projectionKind;
            }
        }
        throw new IllegalStateException(projectionToken.getText());
    }

    @Override
    public <R, D> R accept(@NotNull KtVisitor<R, D> visitor, D data) {
        return visitor.visitTypeProjection(this, data);
    }

    @Nullable
    @SuppressWarnings("deprecation")
    public KtTypeReference getTypeReference() {
        return getStubOrPsiChild(KtStubBasedElementTypes.TYPE_REFERENCE);
    }

    @Nullable
    public PsiElement getProjectionToken() {
        PsiElement star = findChildByType(KtTokens.MUL);
        if (star != null) {
            return star;
        }

        KtModifierList modifierList = getModifierList();
        if (modifierList != null) {
            PsiElement element = modifierList.getModifier(KtTokens.IN_KEYWORD);
            if (element != null) return element;

            element = modifierList.getModifier(KtTokens.OUT_KEYWORD);
            if (element != null) return element;
        }

        return null;
    }
}
