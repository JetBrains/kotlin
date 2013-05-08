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
import com.intellij.navigation.ItemPresentation;
import com.intellij.navigation.ItemPresentationProviders;
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.util.ArrayFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.JetNodeTypes;
import org.jetbrains.jet.lang.psi.stubs.PsiJetParameterStub;
import org.jetbrains.jet.lang.psi.stubs.elements.JetStubElementTypes;
import org.jetbrains.jet.lexer.JetTokens;

public class JetParameter extends JetNamedDeclarationStub<PsiJetParameterStub> {
    public static final JetParameter[] EMPTY_ARRAY = new JetParameter[0];

    public static final ArrayFactory<JetParameter> ARRAY_FACTORY = new ArrayFactory<JetParameter>() {
        @Override
        public JetParameter[] create(int count) {
            return count == 0 ? EMPTY_ARRAY : new JetParameter[count];
        }
    };

    public JetParameter(@NotNull ASTNode node) {
        super(node);
    }

    public JetParameter(@NotNull PsiJetParameterStub stub, @NotNull IStubElementType nodeType) {
        super(stub, nodeType);
    }

    @NotNull
    @Override
    public IStubElementType getElementType() {
        return JetStubElementTypes.VALUE_PARAMETER;
    }

    @Override
    public void accept(@NotNull JetVisitorVoid visitor) {
        visitor.visitParameter(this);
    }

    @Override
    public <R, D> R accept(@NotNull JetVisitor<R, D> visitor, D data) {
        return visitor.visitParameter(this, data);
    }

    @Nullable
    public JetTypeReference getTypeReference() {
        return (JetTypeReference) findChildByType(JetNodeTypes.TYPE_REFERENCE);
    }

    @Nullable
    public JetExpression getDefaultValue() {
        boolean passedEQ = false;
        ASTNode child = getNode().getFirstChildNode();
        while (child != null) {
            if (child.getElementType() == JetTokens.EQ) passedEQ = true;
            if (passedEQ && child.getPsi() instanceof JetExpression) {
                return (JetExpression) child.getPsi();
            }
            child = child.getTreeNext();
        }

        return null;
    }

    public boolean isMutable() {
        PsiJetParameterStub stub = getStub();
        if (stub != null) {
            return stub.isMutable();
        }

        return findChildByType(JetTokens.VAR_KEYWORD) != null;
    }

    public boolean isVarArg() {
        PsiJetParameterStub stub = getStub();
        if (stub != null) {
            return stub.isVarArg();
        }

        JetModifierList modifierList = getModifierList();
        return modifierList != null && modifierList.getModifierNode(JetTokens.VARARG_KEYWORD) != null;
    }

    @Nullable
    public ASTNode getValOrVarNode() {
        ASTNode val = getNode().findChildByType(JetTokens.VAL_KEYWORD);
        if (val != null) return val;

        return getNode().findChildByType(JetTokens.VAR_KEYWORD);
    }

    @Override
    public ItemPresentation getPresentation() {
        return ItemPresentationProviders.getItemPresentation(this);
    }
}
