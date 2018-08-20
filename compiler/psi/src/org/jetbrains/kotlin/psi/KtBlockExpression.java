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
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiModifiableCodeBlock;
import kotlin.annotations.jvm.ReadOnly;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.lexer.KtTokens;

import java.util.Arrays;
import java.util.List;

public class KtBlockExpression extends KtExpressionImpl implements KtStatementExpression, PsiModifiableCodeBlock {
    public KtBlockExpression(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public boolean shouldChangeModificationCount(PsiElement place) {
        // To prevent OutOfBlockModification increase from JavaCodeBlockModificationListener
        return false;
    }

    @Override
    public <R, D> R accept(@NotNull KtVisitor<R, D> visitor, D data) {
        return visitor.visitBlockExpression(this, data);
    }

    @ReadOnly
    @NotNull
    public List<KtExpression> getStatements() {
        return Arrays.asList(findChildrenByClass(KtExpression.class));
    }

    @Nullable
    public TextRange getLastBracketRange() {
        PsiElement rBrace = getRBrace();
        return rBrace != null ? rBrace.getTextRange() : null;
    }

    @Nullable
    public PsiElement getRBrace() {
        return findChildByType(KtTokens.RBRACE);
    }

    @Nullable
    public PsiElement getLBrace() {
        return findChildByType(KtTokens.LBRACE);
    }
}
