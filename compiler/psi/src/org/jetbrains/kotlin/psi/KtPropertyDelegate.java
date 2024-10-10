/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.lexer.KtTokens;

import java.util.Objects;

public class KtPropertyDelegate extends KtElementImpl {
    public KtPropertyDelegate(@NotNull ASTNode node) {
        super(node);
    }

    /**
     * @return null for compiled or incorrect code
     */
    @Nullable
    public KtExpression getExpression() {
        return findChildByClass(KtExpression.class);
    }

    @Override
    public <R, D> R accept(@NotNull KtVisitor<R, D> visitor, D data) {
        return visitor.visitPropertyDelegate(this, data);
    }

    /**
     * @deprecated use `getByKeywordNode().getNode()`
     */
    @Deprecated
    @NotNull
    public ASTNode getByKeywordNode() {
        return getByKeyword().getNode();
    }

    @NotNull
    public PsiElement getByKeyword() {
        return Objects.requireNonNull(findChildByType(KtTokens.BY_KEYWORD));
    }
}
