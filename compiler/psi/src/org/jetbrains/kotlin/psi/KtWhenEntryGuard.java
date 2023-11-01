/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.lexer.KtTokens;

public class KtWhenEntryGuard extends KtElementImpl {
    public KtWhenEntryGuard(@NotNull ASTNode node) {
        super(node);
    }

    @Nullable
    public KtExpression getExpression() {
        return findChildByClass(KtExpression.class);
    }

    @Nullable
    public PsiElement getKeyword() {
        PsiElement andAnd = findChildByType(KtTokens.ANDAND);
        return andAnd == null ? findChildByType(KtTokens.IF_KEYWORD) : andAnd;
    }
}
