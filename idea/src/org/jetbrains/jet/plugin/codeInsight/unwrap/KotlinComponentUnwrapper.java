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
import org.jetbrains.kotlin.psi.JetElement;
import org.jetbrains.kotlin.psi.JetExpression;

public abstract class KotlinComponentUnwrapper extends KotlinUnwrapRemoveBase {
    public KotlinComponentUnwrapper(String key) {
        super(key);
    }

    @Nullable
    protected abstract JetExpression getExpressionToUnwrap(@NotNull JetElement target);

    @NotNull
    protected JetElement getEnclosingElement(@NotNull JetElement element) {
        return element;
    }

    @Override
    public boolean isApplicableTo(PsiElement e) {
        if (!(e instanceof JetElement)) return false;

        JetExpression expressionToUnwrap = getExpressionToUnwrap((JetElement) e);
        return expressionToUnwrap != null && canExtractExpression(expressionToUnwrap,
                                                                  (JetElement) getEnclosingElement((JetElement) e).getParent());
    }

    @Override
    protected void doUnwrap(PsiElement element, Context context) throws IncorrectOperationException {
        JetElement targetElement = (JetElement) element;
        JetExpression expressionToUnwrap = getExpressionToUnwrap(targetElement);
        assert expressionToUnwrap != null;

        JetElement enclosingElement = getEnclosingElement(targetElement);
        context.extractFromExpression(expressionToUnwrap, enclosingElement);
        context.delete(enclosingElement);
    }
}
