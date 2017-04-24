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
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNameIdentifierOwner;
import com.intellij.psi.search.LocalSearchScope;
import com.intellij.psi.search.SearchScope;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class KtLabeledExpression extends KtExpressionWithLabel implements PsiNameIdentifierOwner {
    public KtLabeledExpression(@NotNull ASTNode node) {
        super(node);
    }

    @Nullable @IfNotParsed
    public KtExpression getBaseExpression() {
        return findChildByClass(KtExpression.class);
    }

    @Override
    public <R, D> R accept(@NotNull KtVisitor<R, D> visitor, D data) {
        return visitor.visitLabeledExpression(this, data);
    }

    @Override
    public String getName() {
        return getLabelName();
    }

    @Override
    public PsiElement setName(@NotNull String name) throws IncorrectOperationException {
        KtSimpleNameExpression currentLabel = getTargetLabel();
        if (currentLabel != null) {
            KtSimpleNameExpression newLabel = new KtPsiFactory(getProject()).createLabeledExpression(name).getTargetLabel();
            //noinspection ConstantConditions
            currentLabel.replace(newLabel);
        }
        return this;
    }

    @Nullable
    @Override
    public PsiElement getNameIdentifier() {
        KtSimpleNameExpression targetLabel = getTargetLabel();
        if (targetLabel == null) return null;
        return targetLabel.getIdentifier();
    }

    @NotNull
    @Override
    public SearchScope getUseScope() {
        return new LocalSearchScope(this);
    }
}
