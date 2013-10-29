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
import com.intellij.psi.search.LocalSearchScope;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.JetNodeTypes;
import org.jetbrains.jet.lang.psi.stubs.PsiJetPropertyStub;
import org.jetbrains.jet.lang.psi.stubs.elements.JetStubElementTypes;
import org.jetbrains.jet.lexer.JetTokens;

import java.util.List;

import static org.jetbrains.jet.JetNodeTypes.PROPERTY_ACCESSOR;
import static org.jetbrains.jet.JetNodeTypes.PROPERTY_DELEGATE;
import static org.jetbrains.jet.lexer.JetTokens.*;

public class JetProperty extends JetTypeParameterListOwnerStub<PsiJetPropertyStub> implements JetVariableDeclaration,
                                                                                              JetCallableDeclaration {
    public JetProperty(@NotNull ASTNode node) {
        super(node);
    }

    public JetProperty(@NotNull PsiJetPropertyStub stub, @NotNull IStubElementType nodeType) {
        super(stub, nodeType);
    }

    @Override
    public void accept(@NotNull JetVisitorVoid visitor) {
        visitor.visitProperty(this);
    }

    @Override
    public <R, D> R accept(@NotNull JetVisitor<R, D> visitor, D data) {
        return visitor.visitProperty(this, data);
    }

    @NotNull
    @Override
    public IStubElementType getElementType() {
        return JetStubElementTypes.PROPERTY;
    }

    @Override
    public boolean isVar() {
        PsiJetPropertyStub stub = getStub();
        if (stub != null) {
            return stub.isVar();
        }

        return getNode().findChildByType(JetTokens.VAR_KEYWORD) != null;
    }

    public boolean isLocal() {
        PsiElement parent = getParent();
        return !(parent instanceof JetFile || parent instanceof JetClassBody);
    }

    public boolean isTopLevel() {
        PsiJetPropertyStub stub = getStub();
        if (stub != null) {
            return stub.isTopLevel();
        }

        return getParent() instanceof JetFile;
    }

    @Nullable
    @Override
    public JetParameterList getValueParameterList() {
        return null;
    }

    @Override
    @Nullable
    public JetTypeReference getReceiverTypeRef() {
        ASTNode node = getNode().getFirstChildNode();
        while (node != null) {
            IElementType tt = node.getElementType();
            if (tt == JetTokens.COLON) break;

            if (tt == JetNodeTypes.TYPE_REFERENCE) {
                return (JetTypeReference) node.getPsi();
            }
            node = node.getTreeNext();
        }

        return null;
    }

    @Nullable
    @Override
    public JetTypeReference getReturnTypeRef() {
        return getTypeRef();
    }

    @Override
    @Nullable
    public JetTypeReference getTypeRef() {
        ASTNode node = getNode().getFirstChildNode();
        boolean passedColon = false;
        while (node != null) {
            IElementType tt = node.getElementType();
            if (tt == JetTokens.COLON) {
                passedColon = true;
            }
            else if (tt == JetNodeTypes.TYPE_REFERENCE && passedColon) {
                return (JetTypeReference) node.getPsi();
            }
            node = node.getTreeNext();
        }

        return null;
    }

    @NotNull
    public List<JetPropertyAccessor> getAccessors() {
        return findChildrenByType(PROPERTY_ACCESSOR);
    }

    @Nullable
    public JetPropertyAccessor getGetter() {
        for (JetPropertyAccessor accessor : getAccessors()) {
            if (accessor.isGetter()) return accessor;
        }

        return null;
    }

    @Nullable
    public JetPropertyAccessor getSetter() {
        for (JetPropertyAccessor accessor : getAccessors()) {
            if (accessor.isSetter()) return accessor;
        }

        return null;
    }

    @Nullable
    public JetPropertyDelegate getDelegate() {
        return (JetPropertyDelegate) findChildByType(PROPERTY_DELEGATE);
    }

    @Nullable
    public JetExpression getDelegateExpression() {
        JetPropertyDelegate delegate = getDelegate();
        if (delegate != null) {
            return delegate.getExpression();
        }
        return null;
    }

    @Override
    @Nullable
    public JetExpression getInitializer() {
        return PsiTreeUtil.getNextSiblingOfType(findChildByType(EQ), JetExpression.class);
    }

    @Nullable
    public JetExpression getDelegateExpressionOrInitializer() {
        JetExpression expression = getDelegateExpression();
        if (expression == null) {
            return getInitializer();
        }
        return expression;
    }

    @Override
    @NotNull
    public ASTNode getValOrVarNode() {
        ASTNode node = getNode().findChildByType(TokenSet.create(VAL_KEYWORD, VAR_KEYWORD));
        assert node != null : "Val or var should always exist for property";
        return node;
    }

    @Override
    public ItemPresentation getPresentation() {
        return ItemPresentationProviders.getItemPresentation(this);
    }
}
