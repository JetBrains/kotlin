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
import org.jetbrains.kotlin.idea.JetBundle;
import org.jetbrains.kotlin.idea.refactoring.JetRefactoringUtil;
import org.jetbrains.kotlin.psi.JetBlockExpression;
import org.jetbrains.kotlin.psi.JetElement;
import org.jetbrains.kotlin.psi.JetExpression;

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
        assert e instanceof JetElement;
        return JetBundle.message(key, JetRefactoringUtil.getExpressionShortText((JetElement) e));
    }

    protected boolean canExtractExpression(@NotNull JetExpression expression, @NotNull JetElement parent) {
        if (expression instanceof JetBlockExpression) {
            JetBlockExpression block = (JetBlockExpression) expression;

            return block.getStatements().size() <= 1 || parent instanceof JetBlockExpression;
        }
        return true;
    }

    protected static class Context extends AbstractUnwrapper.AbstractContext {
        @Override
        protected boolean isWhiteSpace(PsiElement element) {
            return element instanceof PsiWhiteSpace;
        }

        public void extractFromBlock(@NotNull JetBlockExpression block, @NotNull JetElement from) throws IncorrectOperationException {
            List<JetExpression> expressions = block.getStatements();
            if (!expressions.isEmpty()) {
                extract(expressions.get(0), expressions.get(expressions.size() - 1), from);
            }
        }

        public void extractFromExpression(@NotNull JetExpression expression, @NotNull JetElement from) throws IncorrectOperationException {
            if (expression instanceof JetBlockExpression) {
                extractFromBlock((JetBlockExpression) expression, from);
            }
            else {
                extract(expression, expression, from);
            }
        }

        public void replace(@NotNull JetElement originalElement, @NotNull JetElement newElement) {
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
