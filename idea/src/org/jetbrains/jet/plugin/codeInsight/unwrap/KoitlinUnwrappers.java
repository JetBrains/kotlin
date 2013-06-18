/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.codeInsight.unwrap;

import com.intellij.psi.PsiElement;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.*;

public class KoitlinUnwrappers {
    private KoitlinUnwrappers() {
    }

    public static class KotlinElseUnwrapper extends KotlinComponentUnwrapper {
        public KotlinElseUnwrapper(String key) {
            super(key);
        }

        @Override
        protected JetExpression getExpressionToUnwrap(@NotNull JetElement target) {
            return target instanceof JetIfExpression ? ((JetIfExpression) target).getElse() : null;
        }
    }

    public static class KotlinThenUnwrapper extends KotlinComponentUnwrapper {
        public KotlinThenUnwrapper(String key) {
            super(key);
        }

        @Override
        protected JetExpression getExpressionToUnwrap(@NotNull JetElement target) {
            return target instanceof JetIfExpression ? ((JetIfExpression) target).getThen() : null;
        }
    }

    public static class KotlinElseRemover extends KotlinUnwrapRemoveBase {
        public KotlinElseRemover(String key) {
            super(key);
        }

        @Override
        public boolean isApplicableTo(PsiElement e) {
            return e instanceof JetIfExpression && ((JetIfExpression) e).getElse() != null;
        }

        @Override
        protected void doUnwrap(PsiElement element, Context context) throws IncorrectOperationException {
            JetIfExpression ifExpr = (JetIfExpression) element;
            context.replace(ifExpr, JetPsiFactory.createIf(ifExpr.getProject(), ifExpr.getCondition(), ifExpr.getThen(), null));
        }
    }

    public static class KotlinLoopUnwrapper extends KotlinComponentUnwrapper {
        public KotlinLoopUnwrapper(String key) {
            super(key);
        }

        @Override
        @Nullable
        protected JetExpression getExpressionToUnwrap(@NotNull JetElement target) {
            return target instanceof JetLoopExpression ? ((JetLoopExpression) target).getBody() : null;
        }
    }
}
