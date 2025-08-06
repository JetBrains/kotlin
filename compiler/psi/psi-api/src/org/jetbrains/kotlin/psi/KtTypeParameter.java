/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.LocalSearchScope;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.KtStubBasedElementTypes;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.psi.stubs.KotlinTypeParameterStub;
import org.jetbrains.kotlin.types.Variance;

public class KtTypeParameter extends KtNamedDeclarationStub<KotlinTypeParameterStub> {

    public KtTypeParameter(@NotNull ASTNode node) {
        super(node);
    }

    public KtTypeParameter(@NotNull KotlinTypeParameterStub stub) {
        super(stub, KtStubBasedElementTypes.TYPE_PARAMETER);
    }

    @Override
    public <R, D> R accept(@NotNull KtVisitor<R, D> visitor, D data) {
        return visitor.visitTypeParameter(this, data);
    }

    @NotNull
    public Variance getVariance() {
        KtModifierList modifierList = getModifierList();
        if (modifierList == null) return Variance.INVARIANT;

        if (modifierList.hasModifier(KtTokens.OUT_KEYWORD)) return Variance.OUT_VARIANCE;
        if (modifierList.hasModifier(KtTokens.IN_KEYWORD)) return Variance.IN_VARIANCE;
        return Variance.INVARIANT;
    }

    @Nullable
    public KtTypeReference setExtendsBound(@Nullable KtTypeReference typeReference) {
        KtTypeReference currentExtendsBound = getExtendsBound();
        if (currentExtendsBound != null) {
            if (typeReference == null) {
                PsiElement colon = findChildByType(KtTokens.COLON);
                if (colon != null) colon.delete();
                currentExtendsBound.delete();
                return null;
            }
            return (KtTypeReference) currentExtendsBound.replace(typeReference);
        }

        if (typeReference != null) {
            PsiElement colon = addAfter(new KtPsiFactory(getProject()).createColon(), getNameIdentifier());
            return (KtTypeReference) addAfter(typeReference, colon);
        }

        return null;
    }

    @Nullable
    public KtTypeReference getExtendsBound() {
        return getStubOrPsiChild(KtStubBasedElementTypes.TYPE_REFERENCE);
    }

    @NotNull
    @Override
    public SearchScope getUseScope() {
        KtTypeParameterListOwner owner = PsiTreeUtil.getParentOfType(this, KtTypeParameterListOwner.class);
        return new LocalSearchScope(owner != null ? owner : this);
    }
}
