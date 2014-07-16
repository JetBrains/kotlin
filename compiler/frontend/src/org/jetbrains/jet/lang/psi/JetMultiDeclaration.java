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
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.TokenSet;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.JetNodeTypes;
import org.jetbrains.jet.lexer.JetTokens;

import java.util.List;

import static org.jetbrains.jet.lexer.JetTokens.*;

public class JetMultiDeclaration extends JetDeclarationImpl {
    public JetMultiDeclaration(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public <R, D> R accept(@NotNull JetVisitor<R, D> visitor, D data) {
        return visitor.visitMultiDeclaration(this, data);
    }

    @NotNull
    public List<JetMultiDeclarationEntry> getEntries() {
        return findChildrenByType(JetNodeTypes.MULTI_VARIABLE_DECLARATION_ENTRY);
    }

    @Nullable
    public JetExpression getInitializer() {
        ASTNode eqNode = getNode().findChildByType(EQ);
        if (eqNode == null) {
            return null;
        }
        return PsiTreeUtil.getNextSiblingOfType(eqNode.getPsi(), JetExpression.class);
    }

    @Nullable
    public ASTNode getValOrVarNode() {
        return getNode().findChildByType(TokenSet.create(VAL_KEYWORD, VAR_KEYWORD));
    }

    @Nullable
    public PsiElement getRPar() {
        return findChildByType(JetTokens.RPAR);
    }

    @Nullable
    public PsiElement getLPar() {
        return findChildByType(JetTokens.LPAR);
    }
}
