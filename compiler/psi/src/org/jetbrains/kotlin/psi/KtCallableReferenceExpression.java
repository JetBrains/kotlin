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
import org.jetbrains.kotlin.resolution.KtResolvableCall;

/**
 * The code example:
 * <pre>{@code
 * fun foo(){}
 * fun main() {
 *     ::foo
 * // ^____^
 * }
 * }</pre>
 */
public class KtCallableReferenceExpression extends KtExpressionImpl implements KtDoubleColonExpression, KtResolvableCall {
    public KtCallableReferenceExpression(@NotNull ASTNode node) {
        super(node);
    }

    @NotNull
    public KtSimpleNameExpression getCallableReference() {
        PsiElement psi = getDoubleColonTokenReference();
        while (psi != null) {
            if (psi instanceof KtSimpleNameExpression) {
                return (KtSimpleNameExpression) psi;
            }
            psi = psi.getNextSibling();
        }

        throw new IllegalStateException("Callable reference simple name shouldn't be parsed to null");
    }

    @Nullable
    @Override
    public PsiElement findColonColon() {
        return findChildByType(KtTokens.COLONCOLON);
    }

    @Override
    public <R, D> R accept(@NotNull KtVisitor<R, D> visitor, D data) {
        return visitor.visitCallableReferenceExpression(this, data);
    }
}
