/*
 * Copyright 2000-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core.surroundWith;

import com.intellij.lang.surroundWith.SurroundDescriptor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.idea.core.util.CodeInsightUtils;
import org.jetbrains.kotlin.psi.KtExpression;

public abstract class KotlinExpressionSurroundDescriptorBase implements SurroundDescriptor {
    @Override
    @NotNull
    public PsiElement[] getElementsToSurround(PsiFile file, int startOffset, int endOffset) {
        KtExpression expression = (KtExpression) CodeInsightUtils.findElement(
                file, startOffset, endOffset, CodeInsightUtils.ElementKind.EXPRESSION);

        return expression == null ? PsiElement.EMPTY_ARRAY : new PsiElement[] {expression};
    }

    @Override
    public boolean isExclusive() {
        return false;
    }
}
