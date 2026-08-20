/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.KtStubBasedElementTypes;
import org.jetbrains.kotlin.psi.stubs.KotlinPlaceHolderStub;

/**
 * Represents a postfix unary expression where the operator follows the operand.
 *
 * <h3>Example:</h3>
 * <pre>{@code
 *    var x = 0
 *    x++
 * // ^_^
 * }</pre>
 */
public class KtPostfixExpression extends KtUnaryExpression {
    @KtImplementationDetail
    public KtPostfixExpression(@NotNull ASTNode node) {
        super(node);
    }

    @KtImplementationDetail
    public KtPostfixExpression(@NotNull KotlinPlaceHolderStub<KtPostfixExpression> stub) {
        super(stub, KtStubBasedElementTypes.POSTFIX_EXPRESSION);
    }

    @Override
    @Nullable @IfNotParsed
    public KtExpression getBaseExpression() {
        KtOperationReferenceExpression operationReference = getOperationReference();
        KtExpression stubBasedOperand = operationReference.getStubBasedOperandBefore$org_jetbrains_kotlin_psi_api();
        if (stubBasedOperand != null) {
            return stubBasedOperand;
        }

        return PsiTreeUtil.getPrevSiblingOfType(operationReference, KtExpression.class);
    }

    @Override
    public <R, D> R accept(@NotNull KtVisitor<R, D> visitor, D data) {
        return visitor.visitPostfixExpression(this, data);
    }
}
