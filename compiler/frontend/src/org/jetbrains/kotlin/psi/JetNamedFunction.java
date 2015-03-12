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
import com.intellij.navigation.ItemPresentation;
import com.intellij.navigation.ItemPresentationProviders;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiModifiableCodeBlock;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.lexer.JetTokens;
import org.jetbrains.kotlin.psi.stubs.KotlinFunctionStub;
import org.jetbrains.kotlin.psi.stubs.elements.JetStubElementTypes;
import org.jetbrains.kotlin.psi.typeRefHelpers.TypeRefHelpersPackage;

import java.util.Collections;
import java.util.List;

public class JetNamedFunction extends JetTypeParameterListOwnerStub<KotlinFunctionStub>
        implements JetFunction, JetWithExpressionInitializer, PsiModifiableCodeBlock {
    public JetNamedFunction(@NotNull ASTNode node) {
        super(node);
    }

    public JetNamedFunction(@NotNull KotlinFunctionStub stub) {
        super(stub, JetStubElementTypes.FUNCTION);
    }

    @Override
    public <R, D> R accept(@NotNull JetVisitor<R, D> visitor, D data) {
        return visitor.visitNamedFunction(this, data);
    }

    public boolean hasTypeParameterListBeforeFunctionName() {
        KotlinFunctionStub stub = getStub();
        if (stub != null) {
            return stub.hasTypeParameterListBeforeFunctionName();
        }
        return hasTypeParameterListBeforeFunctionNameByTree();
    }

    private boolean hasTypeParameterListBeforeFunctionNameByTree() {
        JetTypeParameterList typeParameterList = getTypeParameterList();
        if (typeParameterList == null) {
            return false;
        }
        PsiElement nameIdentifier = getNameIdentifier();
        if (nameIdentifier == null) {
            return true;
        }
        return nameIdentifier.getTextOffset() > typeParameterList.getTextOffset();
    }

    @Override
    public boolean hasBlockBody() {
        KotlinFunctionStub stub = getStub();
        if (stub != null) {
            return stub.hasBlockBody();
        }
        return getEqualsToken() == null;
    }

    @NotNull
    public PsiElement getFunToken() {
        PsiElement element = findChildByType(JetTokens.FUN_KEYWORD);
        assert element != null : "'fun' must be present: " + JetPsiUtil.getElementTextWithContext(this);
        return element;
    }

    @Override
    @Nullable
    public PsiElement getEqualsToken() {
        return findChildByType(JetTokens.EQ);
    }

    @Override
    @Nullable
    public JetExpression getInitializer() {
        return PsiTreeUtil.getNextSiblingOfType(getEqualsToken(), JetExpression.class);
    }

    @Override
    public boolean hasInitializer() {
        return getInitializer() != null;
    }

    @Override
    public ItemPresentation getPresentation() {
        return ItemPresentationProviders.getItemPresentation(this);
    }

    @Override
    @Nullable
    public JetParameterList getValueParameterList() {
        return getStubOrPsiChild(JetStubElementTypes.VALUE_PARAMETER_LIST);
    }

    @Override
    @NotNull
    public List<JetParameter> getValueParameters() {
        JetParameterList list = getValueParameterList();
        return list != null ? list.getParameters() : Collections.<JetParameter>emptyList();
    }

    @Override
    @Nullable
    public JetExpression getBodyExpression() {
        return findChildByClass(JetExpression.class);
    }

    @Override
    public boolean hasBody() {
        KotlinFunctionStub stub = getStub();
        if (stub != null) {
            return stub.hasBody();
        }
        return getBodyExpression() != null;
    }

    @Override
    public boolean hasDeclaredReturnType() {
        return getTypeReference() != null;
    }

    @Override
    @Nullable
    public JetTypeReference getReceiverTypeReference() {
        KotlinFunctionStub stub = getStub();
        if (stub != null) {
            if (!stub.isExtension()) {
                return null;
            }
            List<JetTypeReference> childTypeReferences = getStubOrPsiChildrenAsList(JetStubElementTypes.TYPE_REFERENCE);
            if (!childTypeReferences.isEmpty()) {
                return childTypeReferences.get(0);
            }
            else {
                return null;
            }
        }
        return getReceiverTypeRefByTree();
    }

    @Nullable
    private JetTypeReference getReceiverTypeRefByTree() {
        PsiElement child = getFirstChild();
        while (child != null) {
            IElementType tt = child.getNode().getElementType();
            if (tt == JetTokens.LPAR || tt == JetTokens.COLON) break;
            if (child instanceof JetTypeReference) {
                return (JetTypeReference) child;
            }
            child = child.getNextSibling();
        }

        return null;
    }

    @Override
    @Nullable
    public JetTypeReference getTypeReference() {
        KotlinFunctionStub stub = getStub();
        if (stub != null) {
            List<JetTypeReference> typeReferences = getStubOrPsiChildrenAsList(JetStubElementTypes.TYPE_REFERENCE);
            int returnTypeIndex = stub.isExtension() ? 1 : 0;
            if (returnTypeIndex >= typeReferences.size()) {
                return null;
            }
            return typeReferences.get(returnTypeIndex);
        }
        return TypeRefHelpersPackage.getTypeReference(this);
    }

    @Override
    @Nullable
    public JetTypeReference setTypeReference(@Nullable JetTypeReference typeRef) {
        return TypeRefHelpersPackage.setTypeReference(this, getValueParameterList(), typeRef);
    }

    @Nullable
    @Override
    public PsiElement getColon() {
        return findChildByType(JetTokens.COLON);
    }

    @Override
    public boolean isLocal() {
        PsiElement parent = getParent();
        return !(parent instanceof JetFile || parent instanceof JetClassBody);
    }

    @Override
    public boolean shouldChangeModificationCount(PsiElement place) {
        // Suppress Java check for out-of-block
        return false;
    }
}
