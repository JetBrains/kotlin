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

import java.util.List;

public class KtTryExpression extends KtExpressionImpl {
    public KtTryExpression(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public <R, D> R accept(@NotNull KtVisitor<R, D> visitor, D data) {
        return visitor.visitTryExpression(this, data);
    }

    @NotNull
    public KtBlockExpression getTryBlock() {
        return (KtBlockExpression) findChildByType(KtNodeTypes.BLOCK);
    }

    @NotNull
    public List<KtCatchClause> getCatchClauses() {
        return findChildrenByType(KtNodeTypes.CATCH);
    }

    @Nullable
    public KtFinallySection getFinallyBlock() {
        return (KtFinallySection) findChildByType(KtNodeTypes.FINALLY);
    }

    @Nullable
    public PsiElement getTryKeyword() {
        return findChildByType(KtTokens.TRY_KEYWORD);
    }
}
