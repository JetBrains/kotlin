/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import kotlin.SubclassOptInRequired;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.KtNodeTypes;

import java.util.Collections;
import java.util.List;

import static org.jetbrains.kotlin.psi.psiUtil.KtPsiUtilKt.isKtFile;

/**
 * Base implementation of {@link KtFunction} for functions that are never backed by a stub, such as function literals and
 * anonymous functions.
 *
 * <p>This is an internal implementation base class of the Kotlin PSI, not intended for direct use or subclassing outside of the
 * PSI implementation.
 */
@SuppressWarnings("deprecation")
@SubclassOptInRequired(markerClass = KtImplementationDetail.class)
public abstract class KtFunctionNotStubbed extends KtTypeParameterListOwnerNotStubbed implements KtFunction {

    @KtImplementationDetail
    public KtFunctionNotStubbed(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    @Nullable
    public KtParameterList getValueParameterList() {
        return (KtParameterList) findChildByType(KtNodeTypes.VALUE_PARAMETER_LIST);
    }

    @Override
    @NotNull
    public List<KtParameter> getValueParameters() {
        KtParameterList list = getValueParameterList();
        return list != null ? list.getParameters() : Collections.emptyList();
    }

    @Override
    @Nullable
    public KtExpression getBodyExpression() {
        return findChildByClass(KtExpression.class);
    }

    @Override
    public boolean hasDeclaredReturnType() {
        return false;
    }

    @Override
    @Nullable
    public KtTypeReference getReceiverTypeReference() {
        return null;
    }

    @Override
    @Nullable
    public KtTypeReference getTypeReference() {
        return null;
    }

    @Nullable
    @Override
    public KtTypeReference setTypeReference(@Nullable KtTypeReference typeRef) {
        if (typeRef == null) return null;
        throw new IllegalStateException("Lambda expressions can't have type reference");
    }

    @Nullable
    @Override
    public PsiElement getColon() {
        return null;
    }

    @Override
    public boolean isLocal() {
        PsiElement parent = getParent();
        return !(isKtFile(parent) || parent instanceof KtClassBody);
    }
}
