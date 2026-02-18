/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.KtNodeTypes;

import java.util.Arrays;

/**
 * Represents a binary expression with a left operand, operator, and right operand.
 *
 * <h3>Example:</h3>
 * <pre>{@code
 * val x = 1 + 2
 * //      ^___^
 * }</pre>
 */
public class KtBinaryExpression extends KtExpressionImpl implements KtOperationExpression {
    public KtBinaryExpression(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public <R, D> R accept(@NotNull KtVisitor<R, D> visitor, D data) {
        return visitor.visitBinaryExpression(this, data);
    }

    @Nullable @IfNotParsed
    public KtExpression getLeft() {
        ASTNode node = getOperationReference().getNode().getTreePrev();
        while (node != null) {
            PsiElement psi = node.getPsi();
            if (psi instanceof KtExpression) {
                return (KtExpression) psi;
            }
            node = node.getTreePrev();
        }

        return null;
    }

    @Nullable @IfNotParsed
    public KtExpression getRight() {
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
    @NotNull
    public KtOperationReferenceExpression getOperationReference() {
        PsiElement operationReference = findChildByType(KtNodeTypes.OPERATION_REFERENCE);
        if (operationReference == null) {
            throw new NullPointerException("No operation reference for binary expression: " + Arrays.toString(getChildren()));
        }

        return (KtOperationReferenceExpression) operationReference;
    }

    @NotNull
    public IElementType getOperationToken() {
        return getOperationReference().getReferencedNameElementType();
    }
}

