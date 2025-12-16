/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.stubs.StubElement;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;

public abstract class KtExpressionImplStub<T extends StubElement<?>> extends KtElementImplStub<T> implements KtExpression {
    public KtExpressionImplStub(@NotNull T stub, @NotNull IStubElementType nodeType) {
        super(stub, nodeType);
    }

    public KtExpressionImplStub(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public <R, D> R accept(@NotNull KtVisitor<R, D> visitor, D data) {
        return visitor.visitExpression(this, data);
    }

    @NotNull
    @Override
    public PsiElement replace(@NotNull PsiElement newElement) throws IncorrectOperationException {
        return KtExpressionImpl.Companion.replaceExpression(this, newElement, true, this::rawReplace);
    }

    @NotNull
    public PsiElement rawReplace(@NotNull PsiElement newElement) {
        return super.replace(newElement);
    }
}
