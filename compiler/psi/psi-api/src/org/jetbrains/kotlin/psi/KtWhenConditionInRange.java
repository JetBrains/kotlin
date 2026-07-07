/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.KtNodeTypes;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.resolution.KtResolvableCall;

/**
 * Represents a {@code when} condition that checks membership using {@code in} or {@code !in}.
 *
 * <h3>Example:</h3>
 * <pre>{@code
 * when (x) {
 *     in 1..10 -> "small"
 * //  ^______^
 * }
 * }</pre>
 */
public class KtWhenConditionInRange extends KtWhenCondition implements KtResolvableCall {
    public KtWhenConditionInRange(@NotNull ASTNode node) {
        super(node);
    }

    /**
     * Returns {@code true} if this is a {@code !in} (not-in) check rather than a plain {@code in} check.
     */
    public boolean isNegated() {
        return getOperationReference().getNode().findChildByType(KtTokens.NOT_IN) != null;
    }

    /**
     * Returns the range or container expression tested against (the part after {@code in}), or {@code null} if it is
     * absent in incomplete code.
     */
    @Nullable @IfNotParsed
    public KtExpression getRangeExpression() {
        // Copied from KtBinaryExpression
        ASTNode node = getOperationReference().getNode().getTreeNext();
        while (node != null) {
            PsiElement psi = node.getPsi();
            if (psi instanceof KtExpression) {
                return (KtExpression) psi;
            }
            node = node.getTreeNext();
        }

        return null;
    }

    @Override
    public <R, D> R accept(@NotNull KtVisitor<R, D> visitor, D data) {
        return visitor.visitWhenConditionInRange(this, data);
    }

    /**
     * Returns the {@code in} or {@code !in} operation sign as a reference expression.
     */
    @NotNull
    public KtOperationReferenceExpression getOperationReference() {
        return (KtOperationReferenceExpression) findChildByType(KtNodeTypes.OPERATION_REFERENCE);
    }
}
