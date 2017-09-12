/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.codeInsight.surroundWith.expression;

import com.intellij.lang.surroundWith.SurroundDescriptor;
import com.intellij.lang.surroundWith.Surrounder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.idea.codeInsight.CodeInsightUtils;
import org.jetbrains.kotlin.psi.KtExpression;

public class KotlinExpressionSurroundDescriptor implements SurroundDescriptor {

    private static final Surrounder[] SURROUNDERS = {
            new KotlinNotSurrounder(),
            new KotlinStringTemplateSurrounder(),
            new KotlinParenthesesSurrounder(),
            new KotlinWhenSurrounder() ,
            new KotlinRuntimeTypeCastSurrounder(),
            new KotlinWithIfExpressionSurrounder(/* withElse = */false),
            new KotlinWithIfExpressionSurrounder(/* withElse = */true),
            new KotlinTryCatchExpressionSurrounder(),
            new KotlinTryCatchFinallyExpressionSurrounder(),
            new KotlinIfElseExpressionSurrounder(/* withBraces = */false),
            new KotlinIfElseExpressionSurrounder(/* withBraces = */true)
    };

    @Override
    @NotNull
    public PsiElement[] getElementsToSurround(PsiFile file, int startOffset, int endOffset) {
        KtExpression expression = (KtExpression) CodeInsightUtils.findElement(file,
                                                                              startOffset,
                                                                              endOffset,
                                                                              CodeInsightUtils.ElementKind.EXPRESSION);
        if (expression == null) {
            return PsiElement.EMPTY_ARRAY;
        }
        return new PsiElement[] {expression};
    }

    @Override
    @NotNull
    public Surrounder[] getSurrounders() {
        return SURROUNDERS;
    }

    @Override
    public boolean isExclusive() {
        return false;
    }
}
