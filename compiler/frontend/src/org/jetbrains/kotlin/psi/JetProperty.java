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
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiModifiableCodeBlock;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.JetNodeTypes;
import org.jetbrains.kotlin.lexer.JetTokens;
import org.jetbrains.kotlin.psi.stubs.KotlinPropertyStub;
import org.jetbrains.kotlin.psi.stubs.elements.JetStubElementTypes;
import org.jetbrains.kotlin.psi.typeRefHelpers.TypeRefHelpersPackage;

import java.util.Collections;
import java.util.List;

import static org.jetbrains.kotlin.JetNodeTypes.PROPERTY_DELEGATE;
import static org.jetbrains.kotlin.lexer.JetTokens.*;

public class JetProperty extends JetTypeParameterListOwnerStub<KotlinPropertyStub>
        implements JetVariableDeclaration, PsiModifiableCodeBlock {

    private static final Logger LOG = Logger.getInstance(JetProperty.class);

    public JetProperty(@NotNull ASTNode node) {
        super(node);
    }

    public JetProperty(@NotNull KotlinPropertyStub stub) {
        super(stub, JetStubElementTypes.PROPERTY);
    }

    @Override
    public <R, D> R accept(@NotNull JetVisitor<R, D> visitor, D data) {
        return visitor.visitProperty(this, data);
    }

    @Override
    public boolean isVar() {
        KotlinPropertyStub stub = getStub();
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
        KotlinPropertyStub stub = getStub();
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

    @NotNull
    @Override
    public List<JetParameter> getValueParameters() {
        return Collections.emptyList();
    }

    @Override
    @Nullable
    public JetTypeReference getReceiverTypeReference() {
        KotlinPropertyStub stub = getStub();
        if (stub != null) {
            if (!stub.isExtension()) {
                return null;
            }
            else {
                return getStubOrPsiChild(JetStubElementTypes.TYPE_REFERENCE);
            }
        }
        return getReceiverTypeRefByTree();
    }

    @Nullable
    private JetTypeReference getReceiverTypeRefByTree() {
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

    @Override
    @Nullable
    public JetTypeReference getTypeReference() {
        KotlinPropertyStub stub = getStub();
        if (stub != null) {
            if (!stub.hasReturnTypeRef()) {
                return null;
            }
            else {
                List<JetTypeReference> typeReferences = getStubOrPsiChildrenAsList(JetStubElementTypes.TYPE_REFERENCE);
                int returnTypeRefPositionInPsi = stub.isExtension() ? 1 : 0;
                if (typeReferences.size() <= returnTypeRefPositionInPsi) {
                    LOG.error("Invalid stub structure built for property:\n" + getText());
                    return null;
                }
                return typeReferences.get(returnTypeRefPositionInPsi);
            }
        }
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

    @NotNull
    public List<JetPropertyAccessor> getAccessors() {
        return getStubOrPsiChildrenAsList(JetStubElementTypes.PROPERTY_ACCESSOR);
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

    public boolean hasDelegate() {
        KotlinPropertyStub stub = getStub();
        if (stub != null) {
            return stub.hasDelegate();
        }
        return getDelegate() != null;
    }

    @Nullable
    public JetPropertyDelegate getDelegate() {
        return (JetPropertyDelegate) findChildByType(PROPERTY_DELEGATE);
    }

    public boolean hasDelegateExpression() {
        KotlinPropertyStub stub = getStub();
        if (stub != null) {
            return stub.hasDelegateExpression();
        }
        return getDelegateExpression() != null;
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
    public boolean hasInitializer() {
        KotlinPropertyStub stub = getStub();
        if (stub != null) {
            return stub.hasInitializer();
        }
        return getInitializer() != null;
    }

    @Override
    @Nullable
    public JetExpression getInitializer() {
        return PsiTreeUtil.getNextSiblingOfType(findChildByType(EQ), JetExpression.class);
    }

    public boolean hasDelegateExpressionOrInitializer() {
        return hasDelegateExpression() || hasInitializer();
    }

    @Nullable
    public JetExpression setInitializer(@Nullable JetExpression initializer) {
        JetExpression oldInitializer = getInitializer();

        if (oldInitializer != null) {
            if (initializer != null) {
                return (JetExpression) oldInitializer.replace(initializer);
            }
            else {
                deleteChildRange(findChildByType(EQ), oldInitializer);
                return null;
            }
        }
        else {
            if (initializer != null) {
                PsiElement addAfter = getTypeReference();
                if (addAfter == null) {
                    addAfter = getNameIdentifier();
                }
                PsiElement eq = addAfter(new JetPsiFactory(getProject()).createEQ(), addAfter);
                return (JetExpression) addAfter(initializer, eq);
            }
            else {
                return null;
            }
        }
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

    @Override
    public boolean shouldChangeModificationCount(PsiElement place) {
        // Suppress Java check for out-of-block
        return false;
    }
}
