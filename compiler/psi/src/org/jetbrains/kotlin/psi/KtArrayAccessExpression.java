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

import com.google.common.collect.Lists;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.KtNodeTypes;
import org.jetbrains.kotlin.lexer.KtTokens;

import java.util.Collections;
import java.util.List;

public class KtArrayAccessExpression extends KtExpressionImpl implements KtReferenceExpression {
    public KtArrayAccessExpression(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public <R, D> R accept(@NotNull KtVisitor<R, D> visitor, D data) {
        return visitor.visitArrayAccessExpression(this, data);
    }

    @Nullable @IfNotParsed
    public KtExpression getArrayExpression() {
        return findChildByClass(KtExpression.class);
    }

    @NotNull
    public List<KtExpression> getIndexExpressions() {
        return PsiTreeUtil.getChildrenOfTypeAsList(getIndicesNode(), KtExpression.class);
    }

    @NotNull
    public KtContainerNode getIndicesNode() {
        KtContainerNode indicesNode = findChildByType(KtNodeTypes.INDICES);
        assert indicesNode != null : "Can't be null because of parser";
        return indicesNode;
    }

    @NotNull
    public List<TextRange> getBracketRanges() {
        PsiElement lBracket = getLeftBracket();
        PsiElement rBracket = getRightBracket();
        if (lBracket == null || rBracket == null) {
            return Collections.emptyList();
        }
        return Lists.newArrayList(lBracket.getTextRange(), rBracket.getTextRange());
    }

    @Nullable
    public PsiElement getLeftBracket() {
        return getIndicesNode().findChildByType(KtTokens.LBRACKET);
    }

    @Nullable
    public PsiElement getRightBracket() {
        return getIndicesNode().findChildByType(KtTokens.RBRACKET);
    }
}
