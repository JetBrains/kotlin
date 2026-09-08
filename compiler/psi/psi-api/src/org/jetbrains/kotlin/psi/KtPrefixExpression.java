/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.util.PsiTreeUtil;
import kotlin.SubclassOptInRequired;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.KtNodeTypes;
import org.jetbrains.kotlin.psi.stubs.KotlinPlaceHolderStub;

/**
 * Represents a prefix unary expression where the operator precedes the operand.
 *
 * <h3>Example:</h3>
 * <pre>{@code
 * val x = -5
 * //      ^^
 * }</pre>
 */
@SubclassOptInRequired(markerClass = KtImplementationDetail.class)
public class KtPrefixExpression extends KtUnaryExpression {
    @KtImplementationDetail
    public KtPrefixExpression(@NotNull ASTNode node) {
        super(node);
    }

    @KtImplementationDetail
    public KtPrefixExpression(@NotNull KotlinPlaceHolderStub<KtPrefixExpression> stub) {
        super(stub, KtNodeTypes.PREFIX_EXPRESSION);
    }

    @Override
    public <R, D> R accept(@NotNull KtVisitor<R, D> visitor, D data) {
        return visitor.visitPrefixExpression(this, data);
    }

    @Override
    @Nullable @IfNotParsed
    public KtExpression getBaseExpression() {
        KtOperationReferenceExpression operationReference = getOperationReference();
        KtExpression stubBasedOperand = operationReference.getStubBasedOperandAfter$org_jetbrains_kotlin_psi_api();
        if (stubBasedOperand != null) {
            return stubBasedOperand;
        }

        return PsiTreeUtil.getNextSiblingOfType(operationReference, KtExpression.class);
    }
}
