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

import java.util.List;

public class KtWhenExpression extends KtExpressionImpl {
    public KtWhenExpression(@NotNull ASTNode node) {
        super(node);
    }

    @NotNull
    public List<KtWhenEntry> getEntries() {
        return findChildrenByType(KtNodeTypes.WHEN_ENTRY);
    }

    @Nullable
    public KtProperty getSubjectVariable() {
        return findChildByClass(KtProperty.class);
    }

    @Nullable
    public KtExpression getSubjectExpression() {
        return findChildByClass(KtExpression.class);
    }

    @Override
    public <R, D> R accept(@NotNull KtVisitor<R, D> visitor, D data) {
        return visitor.visitWhenExpression(this, data);
    }

    @NotNull
    public PsiElement getWhenKeyword() {
        //noinspection ConstantConditions
        return findChildByType(KtTokens.WHEN_KEYWORD);
    }

    @Nullable
    public PsiElement getCloseBrace() {
        return findChildByType(KtTokens.RBRACE);
    }

    @Nullable
    public PsiElement getOpenBrace() {
        return findChildByType(KtTokens.LBRACE);
    }

    @Nullable
    public PsiElement getLeftParenthesis() {
        return findChildByType(KtTokens.LPAR);
    }

    @Nullable
    public PsiElement getRightParenthesis() {
        return findChildByType(KtTokens.RPAR);
    }

    @Nullable
    public KtExpression getElseExpression() {
        for (KtWhenEntry entry : getEntries()) {
            if (entry.isElse()) {
                return entry.getExpression();
            }
        }
        return null;
    }
}
