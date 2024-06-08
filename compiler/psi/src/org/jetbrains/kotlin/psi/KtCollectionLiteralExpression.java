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
import org.jetbrains.kotlin.psi.stubs.KotlinCollectionLiteralExpressionStub;
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes;
import org.jetbrains.kotlin.resolution.KtResolvableCall;

import java.util.Arrays;
import java.util.List;

import static org.jetbrains.kotlin.psi.psiUtil.KtPsiUtilKt.getTrailingCommaByClosingElement;

/**
 * The code example:
 * <pre>{@code
 * annotation class AnnoWithArray(val arr: IntArray)
 *
 * @AnnoWithArray([1, 2, 3])
 * //            ^________^
 * fun foo() { }
 * }</pre>
 */
public class KtCollectionLiteralExpression extends KtElementImplStub<KotlinCollectionLiteralExpressionStub>
        implements KtReferenceExpression, KtResolvableCall {
    public KtCollectionLiteralExpression(@NotNull KotlinCollectionLiteralExpressionStub stub) {
        super(stub, KtStubElementTypes.COLLECTION_LITERAL_EXPRESSION);
    }

    public KtCollectionLiteralExpression(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public <R, D> R accept(@NotNull KtVisitor<R, D> visitor, D data) {
        return visitor.visitCollectionLiteralExpression(this, data);
    }

    @Nullable
    public PsiElement getLeftBracket() {
        ASTNode astNode = getNode().findChildByType(KtTokens.LBRACKET);
        return astNode != null ? astNode.getPsi() : null;
    }

    @Nullable
    public PsiElement getRightBracket() {
        ASTNode astNode = getNode().findChildByType(KtTokens.RBRACKET);
        return astNode != null ? astNode.getPsi() : null;
    }

    @Nullable
    public PsiElement getTrailingComma() {
        PsiElement rightBracket = getRightBracket();
        return getTrailingCommaByClosingElement(rightBracket);
    }

    public List<KtExpression> getInnerExpressions() {
        KotlinCollectionLiteralExpressionStub stub = getStub();
        if (stub != null) {
            return Arrays.asList(stub.getChildrenByType(KtStubElementTypes.CONSTANT_EXPRESSIONS_TYPES, KtExpression.EMPTY_ARRAY));
        }
        return PsiTreeUtil.getChildrenOfTypeAsList(this, KtExpression.class);
    }
}