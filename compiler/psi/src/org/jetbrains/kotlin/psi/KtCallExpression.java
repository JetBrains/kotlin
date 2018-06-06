/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.psi;

import com.google.common.collect.Lists;
import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.KtNodeTypes;

import java.util.Collections;
import java.util.List;

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
