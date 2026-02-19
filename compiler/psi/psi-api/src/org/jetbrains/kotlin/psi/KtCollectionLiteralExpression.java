/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.KtStubBasedElementTypes;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.psi.stubs.KotlinCollectionLiteralExpressionStub;
import org.jetbrains.kotlin.psi.stubs.elements.KtTokenSets;
import org.jetbrains.kotlin.resolution.KtResolvableCall;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.jetbrains.kotlin.psi.psiUtil.KtPsiUtilKt.getTrailingCommaByClosingElement;

/**
 * Represents a collection literal expression.
 *
 * <h3>Example:</h3>
 * <pre>{@code
 * annotation class AnnoWithArray(val arr: IntArray)
 *
 * @AnnoWithArray([1, 2, 3])
 * //             ^_______^
 * fun foo() {}
 * }</pre>
 */
public class KtCollectionLiteralExpression extends KtElementImplStub<KotlinCollectionLiteralExpressionStub>
        implements KtReferenceExpression, KtResolvableCall {
    public KtCollectionLiteralExpression(@NotNull KotlinCollectionLiteralExpressionStub stub) {
        super(stub, KtStubBasedElementTypes.COLLECTION_LITERAL_EXPRESSION);
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

    /**
     * @return a list of inner expressions. If no inner expressions are present, an empty list is returned.
     */
    public @NotNull List<KtExpression> getInnerExpressions() {
        KotlinCollectionLiteralExpressionStub stub = getStub();
        if (stub != null) {
            int expressionsCount = stub.getInnerExpressionCount();
            if (expressionsCount == 0) {
                return Collections.emptyList();
            }

            KtExpression[] constantExpressions = stub.getChildrenByType(KtTokenSets.CONSTANT_EXPRESSIONS, KtExpression.EMPTY_ARRAY);
            if (constantExpressions.length == expressionsCount) {
                return Arrays.asList(constantExpressions);
            }
        }

        return PsiTreeUtil.getChildrenOfTypeAsList(this, KtExpression.class);
    }
}