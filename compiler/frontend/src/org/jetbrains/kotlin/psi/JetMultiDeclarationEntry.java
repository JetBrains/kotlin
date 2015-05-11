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
import org.jetbrains.kotlin.JetNodeTypes;
import org.jetbrains.kotlin.lexer.JetTokens;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.psi.typeRefHelpers.TypeRefHelpersPackage;

import java.util.Collections;
import java.util.List;

import static org.jetbrains.kotlin.lexer.JetTokens.VAL_KEYWORD;
import static org.jetbrains.kotlin.lexer.JetTokens.VAR_KEYWORD;

public class JetMultiDeclarationEntry extends JetNamedDeclarationNotStubbed implements JetVariableDeclaration {
    public JetMultiDeclarationEntry(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public JetTypeReference getTypeReference() {
        return TypeRefHelpersPackage.getTypeReference(this);
    }

    @Override
    @Nullable
    public JetTypeReference setTypeReference(@Nullable JetTypeReference typeRef) {
        return TypeRefHelpersPackage.setTypeReference(this, getNameIdentifier(), typeRef);
    }

    @Nullable
    @Override
    public PsiElement getColon() {
        return findChildByType(JetTokens.COLON);
    }

    @Nullable
    @Override
    public JetParameterList getValueParameterList() {
        return null;
    }

    @NotNull
    @Override
    public List<JetParameter> getValueParameters() {
        return Collections.emptyList();
    }

    @Nullable
    @Override
    public JetTypeReference getReceiverTypeReference() {
        return null;
    }

    @Nullable
    @Override
    public JetTypeParameterList getTypeParameterList() {
        return null;
    }

    @Nullable
    @Override
    public JetTypeConstraintList getTypeConstraintList() {
        return null;
    }

    @NotNull
    @Override
    public List<JetTypeConstraint> getTypeConstraints() {
        return Collections.emptyList();
    }

    @NotNull
    @Override
    public List<JetTypeParameter> getTypeParameters() {
        return Collections.emptyList();
    }

    @Override
    public <R, D> R accept(@NotNull JetVisitor<R, D> visitor, D data) {
        return visitor.visitMultiDeclarationEntry(this, data);
    }

    @Override
    public boolean isVar() {
        return getParentNode().findChildByType(JetTokens.VAR_KEYWORD) != null;
    }

    @Nullable
    @Override
    public JetExpression getInitializer() {
        return null;
    }

    @Override
    public boolean hasInitializer() {
        return false;
    }

    @NotNull
    private ASTNode getParentNode() {
        ASTNode parent = getNode().getTreeParent();
        assert parent.getElementType() == JetNodeTypes.MULTI_VARIABLE_DECLARATION;
        return parent;
    }

    @Override
    public ASTNode getValOrVarNode() {
        return getParentNode().findChildByType(TokenSet.create(VAL_KEYWORD, VAR_KEYWORD));
    }

    @Nullable
    @Override
    public FqName getFqName() {
        return null;
    }

    @NotNull
    @Override
    public SearchScope getUseScope() {
        JetElement enclosingBlock = JetPsiUtil.getEnclosingElementForLocalDeclaration(this, false);
        if (enclosingBlock != null) return new LocalSearchScope(enclosingBlock);

        return super.getUseScope();
    }
}
