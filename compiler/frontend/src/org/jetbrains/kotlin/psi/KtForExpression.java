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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.KtNodeTypes;
import org.jetbrains.kotlin.lexer.KtTokens;

public class KtForExpression extends KtLoopExpression {
    public KtForExpression(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public <R, D> R accept(@NotNull KtVisitor<R, D> visitor, D data) {
        return visitor.visitForExpression(this, data);
    }

    @Nullable @IfNotParsed
    public KtParameter getLoopParameter() {
        return (KtParameter) findChildByType(KtNodeTypes.VALUE_PARAMETER);
    }

    @Nullable
    public KtDestructuringDeclaration getDestructuringDeclaration() {
        KtParameter loopParameter = getLoopParameter();
        if (loopParameter == null) return null;
        return loopParameter.getDestructuringDeclaration();
    }

    @Nullable @IfNotParsed
    public KtExpression getLoopRange() {
        return findExpressionUnder(KtNodeTypes.LOOP_RANGE);
    }

    @Nullable @IfNotParsed
    public PsiElement getInKeyword() {
        return findChildByType(KtTokens.IN_KEYWORD);
    }

    @NotNull
    public PsiElement getForKeyword() {
        return findChildByType(KtTokens.FOR_KEYWORD);
    }
}
