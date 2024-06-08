/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi;

import com.google.common.collect.Lists;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.KtNodeTypes;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.psi.psiUtil.KtPsiUtilKt;
import org.jetbrains.kotlin.resolution.KtResolvableCall;

import java.util.Collections;
import java.util.List;

/**
 * The code example:
 * <pre>{@code
 * fun main(list: List<Int>) {
 *     list[0]
 * // ^______^
 * }
 * }</pre>
 */
public class KtArrayAccessExpression extends KtExpressionImpl implements KtReferenceExpression, KtResolvableCall {
    public KtArrayAccessExpression(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public <R, D> R accept(@NotNull KtVisitor<R, D> visitor, D data) {
        return visitor.visitArrayAccessExpression(this, data);
    }

    @Nullable @IfNotParsed
    public KtExpression getArrayExpression() {
        return findChildByClass(KtExpression.class);
    }

    @NotNull
    public List<KtExpression> getIndexExpressions() {
        return PsiTreeUtil.getChildrenOfTypeAsList(getIndicesNode(), KtExpression.class);
    }

    @NotNull
    public KtContainerNode getIndicesNode() {
        KtContainerNode indicesNode = findChildByType(KtNodeTypes.INDICES);
        assert indicesNode != null : "Can't be null because of parser";
        return indicesNode;
    }

    @NotNull
    public List<TextRange> getBracketRanges() {
        PsiElement lBracket = getLeftBracket();
        PsiElement rBracket = getRightBracket();
        if (lBracket == null || rBracket == null) {
            return Collections.emptyList();
        }
        return Lists.newArrayList(lBracket.getTextRange(), rBracket.getTextRange());
    }

    @Nullable
    public PsiElement getLeftBracket() {
        return getIndicesNode().findChildByType(KtTokens.LBRACKET);
    }

    @Nullable
    public PsiElement getRightBracket() {
        return getIndicesNode().findChildByType(KtTokens.RBRACKET);
    }

    public PsiElement getTrailingComma() {
        return KtPsiUtilKt.getTrailingCommaByClosingElement(getRightBracket());
    }
}
