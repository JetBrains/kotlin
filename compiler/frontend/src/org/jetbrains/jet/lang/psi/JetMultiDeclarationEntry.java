/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.JetNodeTypes;
import org.jetbrains.jet.lang.psi.typeRefHelpers.TypeRefHelpersPackage;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lexer.JetTokens;

import static org.jetbrains.jet.lexer.JetTokens.VAL_KEYWORD;
import static org.jetbrains.jet.lexer.JetTokens.VAR_KEYWORD;

public class JetMultiDeclarationEntry extends JetNamedDeclarationNotStubbed implements JetVariableDeclaration {
    public JetMultiDeclarationEntry(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public JetTypeReference getTypeRef() {
        return TypeRefHelpersPackage.getTypeRef(this);
    }

    @Nullable
    public JetTypeReference setTypeRef(@Nullable JetTypeReference typeRef) {
        return TypeRefHelpersPackage.setTypeRef(this, getNameIdentifier(), typeRef);
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
}
