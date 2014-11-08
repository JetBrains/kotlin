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
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.stubs.KotlinParameterStub;
import org.jetbrains.jet.lang.psi.stubs.elements.JetStubElementTypes;
import org.jetbrains.jet.lang.psi.typeRefHelpers.TypeRefHelpersPackage;
import org.jetbrains.jet.lexer.JetTokens;

import java.util.Collections;
import java.util.List;

public class JetParameter extends JetNamedDeclarationStub<KotlinParameterStub> implements JetCallableDeclaration {

    public JetParameter(@NotNull ASTNode node) {
        super(node);
    }

    public JetParameter(@NotNull KotlinParameterStub stub) {
        super(stub, JetStubElementTypes.VALUE_PARAMETER);
    }

    @Override
    public <R, D> R accept(@NotNull JetVisitor<R, D> visitor, D data) {
        return visitor.visitParameter(this, data);
    }

    @Override
    @Nullable
    public JetTypeReference getTypeReference() {
        return getStubOrPsiChild(JetStubElementTypes.TYPE_REFERENCE);
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

    public boolean hasDefaultValue() {
        KotlinParameterStub stub = getStub();
        if (stub != null) {
            return stub.hasDefaultValue();
        }
        return getDefaultValue() != null;
    }

    @Nullable
    public JetExpression getDefaultValue() {
        KotlinParameterStub stub = getStub();
        if (stub != null && !stub.hasDefaultValue()) {
            return null;
        }
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
        KotlinParameterStub stub = getStub();
        if (stub != null) {
            return stub.isMutable();
        }

        return findChildByType(JetTokens.VAR_KEYWORD) != null;
    }

    public boolean isVarArg() {
        JetModifierList modifierList = getModifierList();
        return modifierList != null && modifierList.hasModifier(JetTokens.VARARG_KEYWORD);
    }

    public boolean hasValOrVarNode() {
        KotlinParameterStub stub = getStub();
        if (stub != null) {
            return stub.hasValOrValNode();
        }
        return getValOrVarNode() != null;
    }

    @Nullable
    public ASTNode getValOrVarNode() {
        KotlinParameterStub stub = getStub();
        if (stub != null && !stub.hasValOrValNode()) {
            return null;
        }
        ASTNode val = getNode().findChildByType(JetTokens.VAL_KEYWORD);
        if (val != null) return val;

        return getNode().findChildByType(JetTokens.VAR_KEYWORD);
    }

    @Override
    public ItemPresentation getPresentation() {
        return ItemPresentationProviders.getItemPresentation(this);
    }

    public boolean isLoopParameter() {
        return getParent() instanceof JetForClause;
    }

    @Nullable
    @Override
    public JetParameterList getValueParameterList() {
        return null;
    }

    @Nullable
    @Override
    public JetTypeReference getReceiverTypeReference() {
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
}
