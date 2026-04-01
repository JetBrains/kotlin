/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.impl.source.tree.LazyParseablePsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.KtNodeTypes;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.psi.psiUtil.PsiUtilsKt;

import java.util.List;

/**
 * Represents a lambda expression with optional parameters and a body.
 *
 * <h3>Example:</h3>
 * <pre>{@code
 * val sum = { x: Int, y: Int -> x + y }
 * //        ^_________________________^
 * }</pre>
 */
public class KtLambdaExpression extends LazyParseablePsiElement implements KtExpression {
    public KtLambdaExpression(CharSequence text) {
        super(KtNodeTypes.LAMBDA_EXPRESSION, text);
    }

    @Override
    public <R, D> R accept(@NotNull KtVisitor<R, D> visitor, D data) {
        return visitor.visitLambdaExpression(this, data);
    }

    @NotNull
    public KtFunctionLiteral getFunctionLiteral() {
        return findChildByType(KtNodeTypes.FUNCTION_LITERAL).getPsi(KtFunctionLiteral.class);
    }

    @NotNull
    public List<KtParameter> getValueParameters() {
        return getFunctionLiteral().getValueParameters();
    }

    @Nullable
    public KtBlockExpression getBodyExpression() {
        return getFunctionLiteral().getBodyExpression();
    }

    public boolean hasDeclaredReturnType() {
        return getFunctionLiteral().getTypeReference() != null;
    }

    @NotNull
    public KtElement asElement() {
        return this;
    }

    @NotNull
    public ASTNode getLeftCurlyBrace() {
        return getFunctionLiteral().getNode().findChildByType(KtTokens.LBRACE);
    }

    @Nullable
    public ASTNode getRightCurlyBrace() {
        return getFunctionLiteral().getNode().findChildByType(KtTokens.RBRACE);
    }

    @NotNull
    @Override
    public KtFile getContainingKtFile() {
        return PsiUtilsKt.getContainingKtFile(this);
    }

    @Override
    public <D> void acceptChildren(@NotNull KtVisitor<Void, D> visitor, D data) {
        KtPsiUtil.visitChildren(this, visitor, data);
    }

    @Override
    @SuppressWarnings("unchecked")
    public final void accept(@NotNull PsiElementVisitor visitor) {
        if (visitor instanceof KtVisitor) {
            accept((KtVisitor) visitor, null);
        }
        else {
            visitor.visitElement(this);
        }
    }

    @Override
    public String toString() {
        return getNode().getElementType().toString();
    }

    @NotNull
    @Override
    public KtElement getPsiOrParent() {
        return this;
    }

    @SuppressWarnings({"unused", "MethodMayBeStatic"}) //keep for compatibility with potential plugins
    public boolean shouldChangeModificationCount(PsiElement place) {
        return false;
    }

    public final boolean isTrailingLambdaOnNewLine() {
        PsiElement parent = getParent();

        if (parent instanceof KtLabeledExpression) {
            parent = parent.getParent();
        }

        if (parent instanceof KtLambdaArgument) {
            PsiElement prevSibling = parent.getPrevSibling();
            while (prevSibling != null && !(prevSibling instanceof KtElement)) {
                if (prevSibling instanceof PsiWhiteSpace && prevSibling.textContains('\n')) {
                    return true;
                }
                prevSibling = prevSibling.getPrevSibling();
            }
        }

        return false;
    }
}
