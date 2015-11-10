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

package org.jetbrains.kotlin.idea.codeInsight.unwrap;

import com.intellij.codeInsight.unwrap.AbstractUnwrapper;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.idea.KotlinBundle;
import org.jetbrains.kotlin.idea.refactoring.KotlinRefactoringUtil;
import org.jetbrains.kotlin.psi.KtBlockExpression;
import org.jetbrains.kotlin.psi.KtElement;
import org.jetbrains.kotlin.psi.KtExpression;

import java.util.List;

public abstract class KotlinUnwrapRemoveBase extends AbstractUnwrapper<KotlinUnwrapRemoveBase.Context> {
    private final String key;

    protected KotlinUnwrapRemoveBase(@NotNull String key) {
        /*
        Pass empty description to superclass since actual description
        is computed based on the Psi element at hand
        */
        super("");
        this.key = key;
    }

    @Override
    public String getDescription(PsiElement e) {
        assert e instanceof KtElement;
        return KotlinBundle.message(key, KotlinRefactoringUtil.getExpressionShortText((KtElement) e));
    }

    protected boolean canExtractExpression(@NotNull KtExpression expression, @NotNull KtElement parent) {
        if (expression instanceof KtBlockExpression) {
            KtBlockExpression block = (KtBlockExpression) expression;

            return block.getStatements().size() <= 1 || parent instanceof KtBlockExpression;
        }
        return true;
    }

    protected static class Context extends AbstractUnwrapper.AbstractContext {
        @Override
        protected boolean isWhiteSpace(PsiElement element) {
            return element instanceof PsiWhiteSpace;
        }

        public void extractFromBlock(@NotNull KtBlockExpression block, @NotNull KtElement from) throws IncorrectOperationException {
            List<KtExpression> expressions = block.getStatements();
            if (!expressions.isEmpty()) {
                extract(expressions.get(0), expressions.get(expressions.size() - 1), from);
            }
        }

        public void extractFromExpression(@NotNull KtExpression expression, @NotNull KtElement from) throws IncorrectOperationException {
            if (expression instanceof KtBlockExpression) {
                extractFromBlock((KtBlockExpression) expression, from);
            }
            else {
                extract(expression, expression, from);
            }
        }

        public void replace(@NotNull KtElement originalElement, @NotNull KtElement newElement) {
            if (myIsEffective) {
                originalElement.replace(newElement);
            }
        }
    }

    @Override
    protected Context createContext() {
        return new Context();
    }
}
