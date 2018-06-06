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
import com.intellij.psi.ElementManipulators;
import com.intellij.psi.LiteralTextEscaper;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiLanguageInjectionHost;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.lexer.KtTokens;

public class KtStringTemplateExpression extends KtExpressionImpl implements PsiLanguageInjectionHost {
    private static final TokenSet CLOSE_QUOTE_TOKEN_SET = TokenSet.create(KtTokens.CLOSING_QUOTE);

    public KtStringTemplateExpression(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public <R, D> R accept(@NotNull KtVisitor<R, D> visitor, D data) {
        return visitor.visitStringTemplateExpression(this, data);
    }

    @NotNull
    public KtStringTemplateEntry[] getEntries() {
        return findChildrenByClass(KtStringTemplateEntry.class);
    }

    @Override
    public boolean isValidHost() {
        return getNode().getChildren(CLOSE_QUOTE_TOKEN_SET).length != 0;
    }

    @Override
    public PsiLanguageInjectionHost updateText(@NotNull String text) {
        KtExpression newExpression = new KtPsiFactory(getProject()).createExpressionIfPossible(text);
        if (newExpression instanceof KtStringTemplateExpression) return (KtStringTemplateExpression) replace(newExpression);
        return ElementManipulators.handleContentChange(this, text);
    }

    @NotNull
    @Override
    public LiteralTextEscaper<? extends PsiLanguageInjectionHost> createLiteralTextEscaper() {
        return new KotlinStringLiteralTextEscaper(this);
    }

    public boolean hasInterpolation() {
        for (PsiElement child : getChildren()) {
            if (child instanceof KtSimpleNameStringTemplateEntry || child instanceof KtBlockStringTemplateEntry) {
                return true;
            }
        }

        return false;
    }
}
