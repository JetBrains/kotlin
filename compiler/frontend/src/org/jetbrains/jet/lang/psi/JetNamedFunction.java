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
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.JetNodeTypes;
import org.jetbrains.jet.lang.psi.stubs.PsiJetFunctionStub;
import org.jetbrains.jet.lang.psi.stubs.elements.JetStubElementTypes;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lexer.JetTokens;

import java.util.Collections;
import java.util.List;

public class JetNamedFunction extends JetTypeParameterListOwnerStub<PsiJetFunctionStub> implements JetFunction, JetWithExpressionInitializer {
    public JetNamedFunction(@NotNull ASTNode node) {
        super(node);
    }

    public JetNamedFunction(@NotNull PsiJetFunctionStub stub) {
        super(stub, JetStubElementTypes.FUNCTION);
    }

    @Override
    public void accept(@NotNull JetVisitorVoid visitor) {
        visitor.visitNamedFunction(this);
    }

    @Override
    public <R, D> R accept(@NotNull JetVisitor<R, D> visitor, D data) {
        return visitor.visitNamedFunction(this, data);
    }

    public boolean hasTypeParameterListBeforeFunctionName() {
        JetTypeParameterList typeParameterList = getTypeParameterList();
        if (typeParameterList == null) {
            return false;
        }
        PsiElement nameIdentifier = getNameIdentifier();
        if (nameIdentifier == null) {
            return false;
        }
        return nameIdentifier.getTextOffset() > typeParameterList.getTextOffset();
    }

    @Override
    public boolean hasBlockBody() {
        return getEqualsToken() == null;
    }

    @Nullable
    public PsiElement getEqualsToken() {
        return findChildByType(JetTokens.EQ);
    }

    @Override
    @Nullable
    public JetExpression getInitializer() {
        return PsiTreeUtil.getNextSiblingOfType(getEqualsToken(), JetExpression.class);
    }

    /**
    * Returns full qualified name for function "package_fqn.function_name"
    * Not null for top level functions.
    * @return
    */
    @Nullable
    public FqName getFqName() {
        final PsiJetFunctionStub stub = getStub();
        if (stub != null) {
            return stub.getTopFQName();
        }

        PsiElement parent = getParent();
        if (parent instanceof JetFile) {
            // fqname is different in scripts
            if (((JetFile) parent).getNamespaceHeader() == null) {
                return null;
            }
            JetFile jetFile = (JetFile) parent;
            final FqName fileFQN = JetPsiUtil.getFQName(jetFile);
            return fileFQN.child(getNameAsName());
        }

        return null;
    }

    @NotNull
    @Override
    public IStubElementType getElementType() {
        return JetStubElementTypes.FUNCTION;
    }
    
    @Override
    public ItemPresentation getPresentation() {
        return ItemPresentationProviders.getItemPresentation(this);
    }

    @Override
    @Nullable
    public JetParameterList getValueParameterList() {
        return (JetParameterList) findChildByType(JetNodeTypes.VALUE_PARAMETER_LIST);
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
    public boolean hasDeclaredReturnType() {
        return getReturnTypeRef() != null;
    }

    @Override
    @Nullable
    public JetTypeReference getReceiverTypeRef() {
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
    public JetTypeReference getReturnTypeRef() {
        boolean colonPassed = false;
        PsiElement child = getFirstChild();
        while (child != null) {
            IElementType tt = child.getNode().getElementType();
            if (tt == JetTokens.COLON) {
                colonPassed = true;
            }
            if (colonPassed && child instanceof JetTypeReference) {
                return (JetTypeReference) child;
            }
            child = child.getNextSibling();
        }

        return null;
    }

    @NotNull
    @Override
    public JetElement asElement() {
        return this;
    }

    @Override
    public boolean isLocal() {
        PsiElement parent = getParent();
        return !(parent instanceof JetFile || parent instanceof JetClassBody);
    }
}
