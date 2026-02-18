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

/**
 * Represents a type cast expression using {@code as} or {@code as?}.
 *
 * <h3>Example:</h3>
 * <pre>{@code
 * val str = obj as String
 * //        ^___________^
 * }</pre>
 */
public class KtBinaryExpressionWithTypeRHS extends KtExpressionImpl implements KtOperationExpression {
    public KtBinaryExpressionWithTypeRHS(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public <R, D> R accept(@NotNull KtVisitor<R, D> visitor, D data) {
        return visitor.visitBinaryWithTypeRHSExpression(this, data);
    }

    @NotNull
    public KtExpression getLeft() {
        KtExpression left = findChildByClass(KtExpression.class);
        assert left != null;
        return left;
    }

    @Nullable @IfNotParsed
    public KtTypeReference getRight() {
        ASTNode node = getOperationReference().getNode();
        while (node != null) {
            PsiElement psi = node.getPsi();
            if (psi instanceof KtTypeReference) {
                return (KtTypeReference) psi;
            }
            node = node.getTreeNext();
        }

        return null;
    }

    @Override
    @NotNull
    public KtSimpleNameExpression getOperationReference() {
        return (KtSimpleNameExpression) findChildByType(KtNodeTypes.OPERATION_REFERENCE);
    }

}
