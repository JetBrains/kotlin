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

package org.jetbrains.kotlin.idea.codeInsight.surroundWith.statement;

import com.intellij.lang.surroundWith.SurroundDescriptor;
import com.intellij.lang.surroundWith.Surrounder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.idea.codeInsight.CodeInsightUtils;

public class KotlinStatementSurroundDescriptor implements SurroundDescriptor {

    private static final Surrounder[] SURROUNDERS = {
            new KotlinIfSurrounder(),
            new KotlinIfElseSurrounder(),
            new KotlinFunctionLiteralSurrounder(),
            new KotlinTryFinallySurrounder(),
            new KotlinTryCatchFinallySurrounder(),
            new KotlinTryCatchSurrounder()
    };

    @Override
    @NotNull
    public PsiElement[] getElementsToSurround(PsiFile file, int startOffset, int endOffset) {
        return CodeInsightUtils.findElements(file, startOffset, endOffset, CodeInsightUtils.ElementKind.EXPRESSION);
    }

    @Override
    @NotNull public Surrounder[] getSurrounders() {
        return SURROUNDERS;
    }

    @Override
    public boolean isExclusive() {
        return false;
    }
}
