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
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.KtNodeTypes;
import org.jetbrains.kotlin.lexer.KtTokens;

public class KtValueArgument extends KtElementImpl implements ValueArgument {
    public KtValueArgument(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public <R, D> R accept(@NotNull KtVisitor<R, D> visitor, D data) {
        return visitor.visitArgument(this, data);
    }

    @Override
    @Nullable @IfNotParsed
    public KtExpression getArgumentExpression() {
        return findChildByClass(KtExpression.class);
    }

    @Override
    @Nullable
    public KtValueArgumentName getArgumentName() {
        return (KtValueArgumentName) findChildByType(KtNodeTypes.VALUE_ARGUMENT_NAME);
    }

    @Nullable
    public PsiElement getEqualsToken() {
        return findChildByType(KtTokens.EQ);
    }

    @Override
    public boolean isNamed() {
        return getArgumentName() != null;
    }

    @NotNull
    @Override
    public KtElement asElement() {
        return this;
    }

    @Override
    public LeafPsiElement getSpreadElement() {
        ASTNode node = getNode().findChildByType(KtTokens.MUL);
        return node == null ? null : (LeafPsiElement) node.getPsi();
    }

    @Override
    public boolean isExternal() {
        return false;
    }
}
