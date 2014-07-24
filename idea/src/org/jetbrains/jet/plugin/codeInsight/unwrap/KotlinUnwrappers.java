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

import static org.jetbrains.jet.lang.psi.PsiPackage.JetPsiFactory;

public class KotlinUnwrappers {
    private KotlinUnwrappers() {
    }

    public static class KotlinExpressionRemover extends KotlinRemover {
        public KotlinExpressionRemover(String key) {
            super(key);
        }

        @Override
        public boolean isApplicableTo(PsiElement e) {
            return e instanceof JetExpression && e.getParent() instanceof JetBlockExpression;
        }
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
            context.replace(ifExpr, JetPsiFactory(ifExpr).createIf(ifExpr.getCondition(), ifExpr.getThen(), null));
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

    public static class KotlinTryUnwrapper extends KotlinComponentUnwrapper {
        public KotlinTryUnwrapper(String key) {
            super(key);
        }

        @Override
        @Nullable
        protected JetExpression getExpressionToUnwrap(@NotNull JetElement target) {
            return target instanceof JetTryExpression ? ((JetTryExpression) target).getTryBlock() : null;
        }
    }

    public static class KotlinCatchUnwrapper extends KotlinComponentUnwrapper {
        public KotlinCatchUnwrapper(String key) {
            super(key);
        }

        @NotNull
        @Override
        protected JetElement getEnclosingElement(@NotNull JetElement element) {
            return (JetElement)element.getParent();
        }

        @Override
        protected JetExpression getExpressionToUnwrap(@NotNull JetElement target) {
            return target instanceof JetCatchClause ? ((JetCatchClause) target).getCatchBody() : null;
        }
    }

    public static class KotlinCatchRemover extends KotlinRemover {
        public KotlinCatchRemover(String key) {
            super(key);
        }

        @Override
        public boolean isApplicableTo(PsiElement e) {
            return e instanceof JetCatchClause;
        }
    }

    public static class KotlinFinallyUnwrapper extends KotlinComponentUnwrapper {
        public KotlinFinallyUnwrapper(String key) {
            super(key);
        }

        @Override
        public boolean isApplicableTo(PsiElement e) {
            return super.isApplicableTo(e) && getEnclosingElement((JetElement)e).getParent() instanceof JetBlockExpression;
        }

        @NotNull
        @Override
        protected JetElement getEnclosingElement(@NotNull JetElement element) {
            return (JetElement)element.getParent();
        }

        @Override
        protected JetExpression getExpressionToUnwrap(@NotNull JetElement target) {
            return target instanceof JetFinallySection ? ((JetFinallySection) target).getFinalExpression() : null;
        }
    }

    public static class KotlinFinallyRemover extends KotlinRemover {
        public KotlinFinallyRemover(String key) {
            super(key);
        }

        @Override
        public boolean isApplicableTo(PsiElement e) {
            return e instanceof JetFinallySection;
        }
    }
}
