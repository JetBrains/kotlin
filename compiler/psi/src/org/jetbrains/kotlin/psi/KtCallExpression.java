/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi;

import com.google.common.collect.Lists;
import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.KtNodeTypes;

import java.util.Collections;
import java.util.List;

/**
 * The code example:
 * <pre>{@code
 * fun main() {
 *     println(0)
 * // ^_________^
 * }
 * }</pre>
 */
public class KtCallExpression extends KtExpressionImpl implements KtCallElement, KtReferenceExpression {
    public KtCallExpression(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public <R, D> R accept(@NotNull KtVisitor<R, D> visitor, D data) {
        return visitor.visitCallExpression(this, data);
    }

    @Override
    @Nullable
    public KtExpression getCalleeExpression() {
        return findChildByClass(KtExpression.class);
    }

    @Override
    @Nullable
    public KtValueArgumentList getValueArgumentList() {
        return (KtValueArgumentList) findChildByType(KtNodeTypes.VALUE_ARGUMENT_LIST);
    }

    @Override
    @Nullable
    public KtTypeArgumentList getTypeArgumentList() {
        return (KtTypeArgumentList) findChildByType(KtNodeTypes.TYPE_ARGUMENT_LIST);
    }

    /**
     * Normally there should be only one (or zero) function literal arguments.
     * The returned value is a list for better handling of commonly made mistake of a function taking a lambda and returning another function.
     * Most of users can simply ignore lists of more than one element.
     */
    @Override
    @NotNull
    public List<KtLambdaArgument> getLambdaArguments() {
        return findChildrenByType(KtNodeTypes.LAMBDA_ARGUMENT);
    }

    @Override
    @NotNull
    public List<KtValueArgument> getValueArguments() {
        KtValueArgumentList list = getValueArgumentList();
        List<KtValueArgument> valueArgumentsInParentheses = list != null ? list.getArguments() : Collections.emptyList();
        List<KtLambdaArgument> functionLiteralArguments = getLambdaArguments();
        if (functionLiteralArguments.isEmpty()) {
            return valueArgumentsInParentheses;
        }
        List<KtValueArgument> allValueArguments = Lists.newArrayList();
        allValueArguments.addAll(valueArgumentsInParentheses);
        allValueArguments.addAll(functionLiteralArguments);
        return allValueArguments;
    }

    @Override
    @NotNull
    public List<KtTypeProjection> getTypeArguments() {
        KtTypeArgumentList list = getTypeArgumentList();
        return list != null ? list.getArguments() : Collections.emptyList();
    }
}
