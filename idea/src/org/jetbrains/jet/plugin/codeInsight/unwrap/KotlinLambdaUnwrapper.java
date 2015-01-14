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
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.psi.*;

public class KotlinLambdaUnwrapper extends KotlinUnwrapRemoveBase {
    public KotlinLambdaUnwrapper(String key) {
        super(key);
    }

    private static JetElement getLambdaEnclosingElement(@NotNull JetFunctionLiteralExpression lambda) {
        PsiElement parent = lambda.getParent();

        if (parent instanceof JetValueArgument) {
            return PsiTreeUtil.getParentOfType(parent, JetCallExpression.class, true);
        }

        if (parent instanceof JetCallExpression) {
            return (JetElement) parent;
        }

        if (parent instanceof JetProperty && ((JetProperty) parent).isLocal()) {
            return (JetElement) parent;
        }

        return lambda;
    }

    @Override
    public boolean isApplicableTo(PsiElement e) {
        if (!(e instanceof JetFunctionLiteralExpression)) return false;

        JetFunctionLiteralExpression lambda = (JetFunctionLiteralExpression) e;
        JetBlockExpression body = lambda.getBodyExpression();
        JetElement enclosingElement = getLambdaEnclosingElement((JetFunctionLiteralExpression) e);

        if (body == null || enclosingElement == null) return false;

        return canExtractExpression(body, (JetElement)enclosingElement.getParent());
    }

    @Override
    protected void doUnwrap(PsiElement element, Context context) throws IncorrectOperationException {
        JetFunctionLiteralExpression lambda = (JetFunctionLiteralExpression) element;
        JetBlockExpression body = lambda.getBodyExpression();
        JetElement enclosingExpression = getLambdaEnclosingElement(lambda);

        context.extractFromBlock(body, enclosingExpression);
        context.delete(enclosingExpression);
    }
}
