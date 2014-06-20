/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.JetNodeTypes;
import org.jetbrains.jet.lexer.JetTokens;

import java.util.List;

public class JetWhenExpression extends JetExpressionImpl {
    public JetWhenExpression(@NotNull ASTNode node) {
        super(node);
    }

    @NotNull
    public List<JetWhenEntry> getEntries() {
        return findChildrenByType(JetNodeTypes.WHEN_ENTRY);
    }

    @Nullable
    public JetExpression getSubjectExpression() {
        return findChildByClass(JetExpression.class);
    }

    @Override
    public <R, D> R accept(@NotNull JetVisitor<R, D> visitor, D data) {
        return visitor.visitWhenExpression(this, data);
    }

    @NotNull
    public PsiElement getWhenKeywordElement() {
        //noinspection ConstantConditions
        return findChildByType(JetTokens.WHEN_KEYWORD);
    }

    @Nullable
    public PsiElement getCloseBrace() {
        return findChildByType(JetTokens.RBRACE);
    }

    @Nullable
    public PsiElement getOpenBrace() {
        return findChildByType(JetTokens.LBRACE);
    }

    @Nullable
    public PsiElement getLeftParenthesis() {
        return findChildByType(JetTokens.LPAR);
    }

    @Nullable
    public PsiElement getRightParenthesis() {
        return findChildByType(JetTokens.RPAR);
    }

    @Nullable
    public JetExpression getElseExpression() {
        for (JetWhenEntry entry : getEntries()) {
            if (entry.isElse()) {
                return entry.getExpression();
            }
        }
        return null;
    }
}
