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
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.lexer.JetTokens;

public class JetFunctionLiteral extends JetFunctionNotStubbed {
    public JetFunctionLiteral(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public boolean hasBlockBody() {
        return false;
    }

    @Override
    public String getName() {
        return "<anonymous>";
    }

    @Override
    public PsiElement getNameIdentifier() {
        return null;
    }

    public boolean hasParameterSpecification() {
        return findChildByType(JetTokens.ARROW) != null;
    }

    @Override
    public JetBlockExpression getBodyExpression() {
        return (JetBlockExpression) super.getBodyExpression();
    }

    @Nullable
    @Override
    public PsiElement getEqualsToken() {
        return null;
    }

    @NotNull
    public PsiElement getLBrace() {
        return findChildByType(JetTokens.LBRACE);
    }

    @Nullable
    @IfNotParsed
    public PsiElement getRBrace() {
        return findChildByType(JetTokens.RBRACE);
    }

    @Nullable
    public PsiElement getArrow() {
        return findChildByType(JetTokens.ARROW);
    }

    @Nullable
    @Override
    public FqName getFqName() {
        return null;
    }

    @Override
    public boolean hasBody() {
        return getBodyExpression() != null;
    }
}
