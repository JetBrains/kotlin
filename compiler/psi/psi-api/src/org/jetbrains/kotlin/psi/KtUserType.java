/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi;

import com.google.common.collect.Lists;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.KtStubBasedElementTypes;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.psi.stubs.KotlinUserTypeStub;

import java.util.Collections;
import java.util.List;

/**
 * Represents a simple type, optionally with type arguments.
 *
 * <h3>Example:</h3>
 * <pre>{@code
 * val list: List<String> = listOf()
 * //        ^__________^
 * }</pre>
 */
public class KtUserType extends KtElementImplStub<KotlinUserTypeStub> implements KtTypeElement {
    public KtUserType(@NotNull ASTNode node) {
        super(node);
    }

    public KtUserType(@NotNull KotlinUserTypeStub stub) {
        super(stub, KtStubBasedElementTypes.USER_TYPE);
    }

    @Override
    public <R, D> R accept(@NotNull KtVisitor<R, D> visitor, D data) {
        return visitor.visitUserType(this, data);
    }

    @Nullable
    @SuppressWarnings("deprecation") // KT-78356
    public KtTypeArgumentList getTypeArgumentList() {
        return getStubOrPsiChild(KtStubBasedElementTypes.TYPE_ARGUMENT_LIST);
    }

    @NotNull
    public List<KtTypeProjection> getTypeArguments() {
        // TODO: empty elements in PSI
        KtTypeArgumentList typeArgumentList = getTypeArgumentList();
        return typeArgumentList == null ? Collections.emptyList() : typeArgumentList.getArguments();
    }

    @NotNull
    @Override
    public List<KtTypeReference> getTypeArgumentsAsTypes() {
        List<KtTypeReference> result = Lists.newArrayList();
        for (KtTypeProjection projection : getTypeArguments()) {
            result.add(projection.getTypeReference());
        }
        return result;
    }

    @Nullable @IfNotParsed
    @SuppressWarnings("deprecation") // KT-78356
    public KtSimpleNameExpression getReferenceExpression() {
        KtNameReferenceExpression nameRefExpr = getStubOrPsiChild(KtStubBasedElementTypes.REFERENCE_EXPRESSION);
        return nameRefExpr != null ? nameRefExpr : getStubOrPsiChild(KtStubBasedElementTypes.ENUM_ENTRY_SUPERCLASS_REFERENCE_EXPRESSION);
    }

    @Nullable
    @SuppressWarnings("deprecation") // KT-78356
    public KtUserType getQualifier() {
        return getStubOrPsiChild(KtStubBasedElementTypes.USER_TYPE);
    }

    public void deleteQualifier() {
        KtUserType qualifier = getQualifier();
        assert qualifier != null;
        PsiElement dot = findChildByType(KtTokens.DOT);
        assert dot != null;
        qualifier.delete();
        dot.delete();
    }

    @Nullable
    public String getReferencedName() {
        KtSimpleNameExpression referenceExpression = getReferenceExpression();
        return referenceExpression == null ? null : referenceExpression.getReferencedName();
    }
}
