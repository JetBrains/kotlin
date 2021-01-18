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
import com.intellij.util.AstLoadingFilter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.KtNodeTypes;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.psi.stubs.KotlinPropertyStub;
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes;
import org.jetbrains.kotlin.psi.typeRefHelpers.TypeRefHelpersKt;

import java.util.Collections;
import java.util.List;

import static org.jetbrains.kotlin.KtNodeTypes.PROPERTY_DELEGATE;
import static org.jetbrains.kotlin.lexer.KtTokens.EQ;

public class KtProperty extends KtTypeParameterListOwnerStub<KotlinPropertyStub>
        implements KtVariableDeclaration, PsiModifiableCodeBlock {

    private static final Logger LOG = Logger.getInstance(KtProperty.class);

    public KtProperty(@NotNull ASTNode node) {
        super(node);
    }

    public KtProperty(@NotNull KotlinPropertyStub stub) {
        super(stub, KtStubElementTypes.PROPERTY);
    }

    @Override
    public <R, D> R accept(@NotNull KtVisitor<R, D> visitor, D data) {
        return visitor.visitProperty(this, data);
    }

    @Override
    public boolean isVar() {
        KotlinPropertyStub stub = getStub();
        if (stub != null) {
            return stub.isVar();
        }

        return getNode().findChildByType(KtTokens.VAR_KEYWORD) != null;
    }

    public boolean isLocal() {
        return !isTopLevel() && !isMember();
    }

    public boolean isMember() {
        PsiElement parent = getParent();
        return parent instanceof KtClassOrObject || parent instanceof KtClassBody ||
               parent instanceof KtBlockExpression && parent.getParent() instanceof KtScript;
    }

    public boolean isTopLevel() {
        KotlinPropertyStub stub = getStub();
        if (stub != null) {
            return stub.isTopLevel();
        }

        return getParent() instanceof KtFile;
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

    @Override
    @Nullable
    public KtTypeReference getReceiverTypeReference() {
        KotlinPropertyStub stub = getStub();
        if (stub != null) {
            if (!stub.isExtension()) {
                return null;
            }
            else {
                return getStubOrPsiChild(KtStubElementTypes.TYPE_REFERENCE);
            }
        }
        return getReceiverTypeRefByTree();
    }

    @NotNull
    @Override
    public List<KtContextReceiver> getContextReceivers() {
        KotlinPropertyStub stub = getStub();
        if (stub != null) {
            KtContextReceiverList contextReceiver = getStubOrPsiChild(KtStubElementTypes.CONTEXT_RECEIVER_LIST);
            if (contextReceiver != null) {
                return contextReceiver.contextReceivers();
            } else {
                return Collections.emptyList();
            }
        }
        return getContextReceiverTypeRefsByTree();
    }

    @NotNull
    private List<KtContextReceiver> getContextReceiverTypeRefsByTree() {
        ASTNode node = getNode().getFirstChildNode();
        while (node != null) {
            IElementType tt = node.getElementType();
            if (tt == KtTokens.COLON) {
                break;
            }
            if (tt == KtNodeTypes.CONTEXT_RECEIVER_LIST) {
                KtContextReceiverList contextReceiver = (KtContextReceiverList) node.getPsi();
                return contextReceiver.contextReceivers();
            }
            node = node.getTreeNext();
        }
        return Collections.emptyList();
    }

    @Nullable
    private KtTypeReference getReceiverTypeRefByTree() {
        ASTNode node = getNode().getFirstChildNode();
        while (node != null) {
            IElementType tt = node.getElementType();
            if (tt == KtTokens.COLON) break;

            if (tt == KtNodeTypes.TYPE_REFERENCE) {
                return (KtTypeReference) node.getPsi();
            }
            node = node.getTreeNext();
        }

        return null;
    }

    @Override
    @Nullable
    public KtTypeReference getTypeReference() {
        KotlinPropertyStub stub = getStub();
        if (stub != null) {
            if (!stub.hasReturnTypeRef()) {
                return null;
            }
            else {
                List<KtTypeReference> typeReferences = getStubOrPsiChildrenAsList(KtStubElementTypes.TYPE_REFERENCE);
                int returnTypeRefPositionInPsi = stub.isExtension() ? 1 : 0;
                if (typeReferences.size() <= returnTypeRefPositionInPsi) {
                    LOG.error("Invalid stub structure built for property:\n" + getText());
                    return null;
                }
                return typeReferences.get(returnTypeRefPositionInPsi);
            }
        }
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
    public PsiElement getEqualsToken() {
        return findChildByType(KtTokens.EQ);
    }

    @NotNull
    public List<KtPropertyAccessor> getAccessors() {
        return getStubOrPsiChildrenAsList(KtStubElementTypes.PROPERTY_ACCESSOR);
    }

    @Nullable
    public KtPropertyAccessor getGetter() {
        for (KtPropertyAccessor accessor : getAccessors()) {
            if (accessor.isGetter()) return accessor;
        }

        return null;
    }

    @Nullable
    public KtPropertyAccessor getSetter() {
        for (KtPropertyAccessor accessor : getAccessors()) {
            if (accessor.isSetter()) return accessor;
        }

        return null;
    }

    @Nullable
    public KtBackingField getFieldDeclaration() {
        for (KtBackingField field : getStubOrPsiChildrenAsList(KtStubElementTypes.BACKING_FIELD)) {
            return field;
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
    public KtPropertyDelegate getDelegate() {
        KotlinPropertyStub stub = getStub();
        if (stub != null && !stub.hasDelegate()) {
            return null;
        }

        return (KtPropertyDelegate) findChildByType(PROPERTY_DELEGATE);
    }

    public boolean hasDelegateExpression() {
        KotlinPropertyStub stub = getStub();
        if (stub != null) {
            return stub.hasDelegateExpression();
        }

        return getDelegateExpression() != null;
    }

    @Nullable
    public KtExpression getDelegateExpression() {
        KotlinPropertyStub stub = getStub();
        if (stub != null && !stub.hasDelegateExpression()) {
            return null;
        }

        KtPropertyDelegate delegate = getDelegate();
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
    public KtExpression getInitializer() {
        KotlinPropertyStub stub = getStub();
        if (stub != null && !stub.hasInitializer()) {
            return null;
        }

        return AstLoadingFilter.forceAllowTreeLoading(this.getContainingFile(), () ->
                PsiTreeUtil.getNextSiblingOfType(findChildByType(EQ), KtExpression.class)
        );
    }

    public boolean hasDelegateExpressionOrInitializer() {
        return hasDelegateExpression() || hasInitializer();
    }

    @Nullable
    public KtExpression setInitializer(@Nullable KtExpression initializer) {
        KtExpression oldInitializer = getInitializer();

        if (oldInitializer != null) {
            if (initializer != null) {
                return (KtExpression) oldInitializer.replace(initializer);
            }
            else {
                PsiElement nextSibling = oldInitializer.getNextSibling();
                PsiElement last =
                        nextSibling != null
                        && nextSibling.getNode() != null
                        && nextSibling.getNode().getElementType() == KtTokens.SEMICOLON
                        ? nextSibling : oldInitializer;

                deleteChildRange(findChildByType(EQ), last);
                return null;
            }
        }
        else {
            if (initializer != null) {
                PsiElement addAfter = getTypeReference();
                if (addAfter == null) {
                    addAfter = getNameIdentifier();
                }
                PsiElement eq = addAfter(new KtPsiFactory(getProject()).createEQ(), addAfter);
                return (KtExpression) addAfter(initializer, eq);
            }
            else {
                return null;
            }
        }
    }

    @Nullable
    public KtExpression getDelegateExpressionOrInitializer() {
        KtExpression expression = getDelegateExpression();
        if (expression == null) {
            return getInitializer();
        }
        return expression;
    }

    @Override
    @NotNull
    public PsiElement getValOrVarKeyword() {
        PsiElement element = findChildByType(VAL_VAR_TOKEN_SET);
        assert element != null : "Val or var should always exist for property" + this.getText();
        return element;
    }

    private static final TokenSet VAL_VAR_TOKEN_SET = TokenSet.create(KtTokens.VAL_KEYWORD, KtTokens.VAR_KEYWORD);

    @Override
    public ItemPresentation getPresentation() {
        return ItemPresentationProviders.getItemPresentation(this);
    }

    @Override
    public boolean shouldChangeModificationCount(PsiElement place) {
        // Suppress Java check for out-of-block
        return false;
    }

    public boolean hasBody() {
        if (hasDelegateExpressionOrInitializer()) return true;
        KtPropertyAccessor getter = getGetter();
        if (getter != null && getter.hasBody()) {
            return true;
        }
        KtPropertyAccessor setter = getSetter();
        if (setter != null && setter.hasBody()) {
            return true;
        }
        return false;
    }
}
