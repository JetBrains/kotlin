/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.psi.stubs.KotlinConstantExpressionStub;
import org.jetbrains.kotlin.psi.utils.ConstantExpressionUtils;

public class KtConstantExpression
        extends KtElementImplStub<KotlinConstantExpressionStub> implements KtExpression {
    public KtConstantExpression(@NotNull ASTNode node) {
        super(node);
    }

    public KtConstantExpression(@NotNull KotlinConstantExpressionStub stub) {
        super(stub, ConstantExpressionUtils.toConstantExpressionElementType(stub.getKind()));
    }

    @Override
    public <R, D> R accept(@NotNull KtVisitor<R, D> visitor, D data) {
        return visitor.visitConstantExpression(this, data);
    }

    @Override
    public PsiElement replace(@NotNull PsiElement newElement) throws IncorrectOperationException {
        return KtExpressionImpl.Companion.replaceExpression(this, newElement, true, super::replace);
    }

    @Override
    public @NotNull String getText() {
        KotlinConstantExpressionStub stub = getGreenStub();
        if (stub != null) {
            return stub.getValue();
        }

        return super.getText();
    }
}
