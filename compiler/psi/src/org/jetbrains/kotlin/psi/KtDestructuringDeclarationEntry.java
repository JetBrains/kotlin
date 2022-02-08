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
import com.intellij.psi.search.LocalSearchScope;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.KtNodeTypes;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.psi.typeRefHelpers.TypeRefHelpersKt;

import java.util.Collections;
import java.util.List;

import static org.jetbrains.kotlin.lexer.KtTokens.VAL_KEYWORD;
import static org.jetbrains.kotlin.lexer.KtTokens.VAR_KEYWORD;

@SuppressWarnings("deprecation")
public class KtDestructuringDeclarationEntry extends KtNamedDeclarationNotStubbed implements KtVariableDeclaration {
    public KtDestructuringDeclarationEntry(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public KtTypeReference getTypeReference() {
        return TypeRefHelpersKt.getTypeReference(this);
    }

    @Override
    @Nullable
    public KtTypeReference setTypeReference(@Nullable KtTypeReference typeRef) {
        return TypeRefHelpersKt.setTypeReference(this, getNameIdentifier(), typeRef);
    }

    @Nullable
    @Override
    public PsiElement getColon() {
        return findChildByType(KtTokens.COLON);
    }

    @Nullable
    @Override
    public KtParameterList getValueParameterList() {
        return null;
    }

    @NotNull
    @Override
    public List<KtParameter> getValueParameters() {
        return Collections.emptyList();
    }

    @Nullable
    @Override
    public KtTypeReference getReceiverTypeReference() {
        return null;
    }

    @NotNull
    @Override
    public List<KtContextReceiver> getContextReceivers() {
        return Collections.emptyList();
    }

    @Nullable
    @Override
    public KtTypeParameterList getTypeParameterList() {
        return null;
    }

    @Nullable
    @Override
    public KtTypeConstraintList getTypeConstraintList() {
        return null;
    }

    @NotNull
    @Override
    public List<KtTypeConstraint> getTypeConstraints() {
        return Collections.emptyList();
    }

    @NotNull
    @Override
    public List<KtTypeParameter> getTypeParameters() {
        return Collections.emptyList();
    }

    @Override
    public <R, D> R accept(@NotNull KtVisitor<R, D> visitor, D data) {
        return visitor.visitDestructuringDeclarationEntry(this, data);
    }

    @Override
    public boolean isVar() {
        return getParentNode().findChildByType(KtTokens.VAR_KEYWORD) != null;
    }

    @Nullable
    @Override
    public KtExpression getInitializer() {
        return null;
    }

    @Override
    public boolean hasInitializer() {
        return false;
    }

    @NotNull
    private ASTNode getParentNode() {
        ASTNode parent = getNode().getTreeParent();
        assert parent.getElementType() == KtNodeTypes.DESTRUCTURING_DECLARATION :
                "parent is " + parent.getElementType();
        return parent;
    }

    @Override
    public PsiElement getValOrVarKeyword() {
        ASTNode node = getParentNode().findChildByType(TokenSet.create(VAL_KEYWORD, VAR_KEYWORD));
        if (node == null) return null;
        return node.getPsi();
    }

    @Nullable
    @Override
    public FqName getFqName() {
        return null;
    }

    @NotNull
    @Override
    public SearchScope getUseScope() {
        KtElement enclosingBlock = KtPsiUtil.getEnclosingElementForLocalDeclaration(this, false);
        if (enclosingBlock instanceof KtParameter) {
            enclosingBlock = KtPsiUtil.getEnclosingElementForLocalDeclaration((KtParameter) enclosingBlock, false);
        }
        if (enclosingBlock != null) return new LocalSearchScope(enclosingBlock);

        return super.getUseScope();
    }
}
