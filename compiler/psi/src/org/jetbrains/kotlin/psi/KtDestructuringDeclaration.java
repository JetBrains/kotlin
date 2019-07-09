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
import com.intellij.psi.tree.TokenSet;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.KtNodeTypes;
import org.jetbrains.kotlin.lexer.KtTokens;

import java.util.List;

import static org.jetbrains.kotlin.lexer.KtTokens.*;

public class KtDestructuringDeclaration extends KtDeclarationImpl implements KtValVarKeywordOwner, KtDeclarationWithInitializer {
    public KtDestructuringDeclaration(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public <R, D> R accept(@NotNull KtVisitor<R, D> visitor, D data) {
        return visitor.visitDestructuringDeclaration(this, data);
    }

    @NotNull
    public List<KtDestructuringDeclarationEntry> getEntries() {
        return findChildrenByType(KtNodeTypes.DESTRUCTURING_DECLARATION_ENTRY);
    }

    @Nullable
    @Override
    public KtExpression getInitializer() {
        ASTNode eqNode = getNode().findChildByType(EQ);
        if (eqNode == null) {
            return null;
        }
        return PsiTreeUtil.getNextSiblingOfType(eqNode.getPsi(), KtExpression.class);
    }

    @Override
    public boolean hasInitializer() {
        return getInitializer() != null;
    }

    public boolean isVar() {
        return getNode().findChildByType(KtTokens.VAR_KEYWORD) != null;
    }

    @Override
    @Nullable
    public PsiElement getValOrVarKeyword() {
        return findChildByType(TokenSet.create(VAL_KEYWORD, VAR_KEYWORD));
    }

    @Nullable
    public PsiElement getRPar() {
        return findChildByType(KtTokens.RPAR);
    }

    @Nullable
    public PsiElement getLPar() {
        return findChildByType(KtTokens.LPAR);
    }
}
