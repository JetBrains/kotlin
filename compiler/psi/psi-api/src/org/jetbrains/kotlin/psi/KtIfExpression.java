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

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.KtNodeTypes;
import org.jetbrains.kotlin.lexer.KtTokens;

public class KtIfExpression extends KtExpressionImpl {
    public KtIfExpression(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public <R, D> R accept(@NotNull KtVisitor<R, D> visitor, D data) {
        return visitor.visitIfExpression(this, data);
    }

    @Nullable @IfNotParsed
    public KtExpression getCondition() {
        return findExpressionUnder(KtNodeTypes.CONDITION);
    }

    @Nullable @IfNotParsed
    public PsiElement getLeftParenthesis() {
        return findChildByType(KtTokens.LPAR);
    }

    @Nullable @IfNotParsed
    public PsiElement getRightParenthesis() {
        return findChildByType(KtTokens.RPAR);
    }

    @Nullable
    public KtExpression getThen() {
        return findExpressionUnder(KtNodeTypes.THEN);
    }

    @Nullable
    public KtExpression getElse() {
        return findExpressionUnder(KtNodeTypes.ELSE);
    }

    @Nullable
    public PsiElement getElseKeyword() {
        return findChildByType(KtTokens.ELSE_KEYWORD);
    }

    @NotNull
    public PsiElement getIfKeyword() {
        return findChildByType(KtTokens.IF_KEYWORD);
    }
}
