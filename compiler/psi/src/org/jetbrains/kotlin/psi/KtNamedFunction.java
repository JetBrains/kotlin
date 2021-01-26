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
import com.intellij.util.AstLoadingFilter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.KtNodeTypes;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.psi.psiUtil.KtPsiUtilKt;
import org.jetbrains.kotlin.psi.stubs.KotlinFunctionStub;
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes;
import org.jetbrains.kotlin.psi.typeRefHelpers.TypeRefHelpersKt;

import java.util.Collections;
import java.util.List;

public class KtNamedFunction extends KtTypeParameterListOwnerStub<KotlinFunctionStub>
        implements KtFunction, KtDeclarationWithInitializer, PsiModifiableCodeBlock {
    public KtNamedFunction(@NotNull ASTNode node) {
        super(node);
    }

    public KtNamedFunction(@NotNull KotlinFunctionStub stub) {
        super(stub, KtStubElementTypes.FUNCTION);
    }

    @Override
    public <R, D> R accept(@NotNull KtVisitor<R, D> visitor, D data) {
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
        KtTypeParameterList typeParameterList = getTypeParameterList();
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

    @Nullable
    @IfNotParsed // "function" with no "fun" keyword is created by parser for "{...}" on top-level or in class body
    public PsiElement getFunKeyword() {
        return findChildByType(KtTokens.FUN_KEYWORD);
    }

    @Override
    @Nullable
    public PsiElement getEqualsToken() {
        return findChildByType(KtTokens.EQ);
    }

    @Override
    @Nullable
    public KtExpression getInitializer() {
        return PsiTreeUtil.getNextSiblingOfType(getEqualsToken(), KtExpression.class);
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
    public KtParameterList getValueParameterList() {
        return getStubOrPsiChild(KtStubElementTypes.VALUE_PARAMETER_LIST);
    }

    @Override
    @NotNull
    public List<KtParameter> getValueParameters() {
        KtParameterList list = getValueParameterList();
        return list != null ? list.getParameters() : Collections.emptyList();
    }

    @Override
    @Nullable
    public KtExpression getBodyExpression() {
        KotlinFunctionStub stub = getStub();
        if (stub != null && !stub.hasBody()) {
            return null;
        }

        return AstLoadingFilter.forceAllowTreeLoading(this.getContainingFile(), () ->
                findChildByClass(KtExpression.class)
        );
    }

    @Nullable
    @Override
    public KtBlockExpression getBodyBlockExpression() {
        KotlinFunctionStub stub = getStub();
        if (stub != null && !(stub.hasBlockBody() && stub.hasBody())) {
            return null;
        }

        KtExpression bodyExpression = findChildByClass(KtExpression.class);
        if (bodyExpression instanceof KtBlockExpression) {
            return (KtBlockExpression) bodyExpression;
        }

        return null;
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
    public KtTypeReference getReceiverTypeReference() {
        KotlinFunctionStub stub = getStub();
        if (stub != null) {
            if (!stub.isExtension()) {
                return null;
            }
            List<KtTypeReference> childTypeReferences = getStubOrPsiChildrenAsList(KtStubElementTypes.TYPE_REFERENCE);
            if (!childTypeReferences.isEmpty()) {
                return childTypeReferences.get(0);
            }
            else {
                return null;
            }
        }
        return getReceiverTypeRefByTree();
    }

    @NotNull
    @Override
    public List<KtTypeReference> getContextReceiverTypeReferences() {
        KotlinFunctionStub stub = getStub();
        if (stub != null) {
            List<KtContextReceiver> childContextReceivers = getStubOrPsiChildrenAsList(KtStubElementTypes.CONTEXT_RECEIVER);
            if (!childContextReceivers.isEmpty()) {
                return childContextReceivers.get(0).typeReferences();
            }
            else {
                return Collections.emptyList();
            }
        }
        return getContextReceiverTypeRefsByTree();
    }

    @NotNull
    private List<KtTypeReference> getContextReceiverTypeRefsByTree() {
        PsiElement child = getFirstChild();
        while (child != null) {
            IElementType tt = child.getNode().getElementType();
            if (tt == KtTokens.LPAR || tt == KtTokens.COLON) break;
            if (child instanceof KtContextReceiver) {
                return ((KtContextReceiver) child).typeReferences();
            }
            child = child.getNextSibling();
        }

        return Collections.emptyList();
    }

    @Nullable
    private KtTypeReference getReceiverTypeRefByTree() {
        PsiElement child = getFirstChild();
        while (child != null) {
            IElementType tt = child.getNode().getElementType();
            if (tt == KtTokens.LPAR || tt == KtTokens.COLON) break;
            if (child instanceof KtTypeReference) {
                return (KtTypeReference) child;
            }
            child = child.getNextSibling();
        }

        return null;
    }

    @Override
    @Nullable
    public KtTypeReference getTypeReference() {
        KotlinFunctionStub stub = getStub();
        if (stub != null) {
            List<KtTypeReference> typeReferences = getStubOrPsiChildrenAsList(KtStubElementTypes.TYPE_REFERENCE);
            int returnTypeIndex = stub.isExtension() ? 1 : 0;
            if (returnTypeIndex >= typeReferences.size()) {
                return null;
            }
            return typeReferences.get(returnTypeIndex);
        }
        return TypeRefHelpersKt.getTypeReference(this);
    }

    @Override
    @Nullable
    public KtTypeReference setTypeReference(@Nullable KtTypeReference typeRef) {
        return TypeRefHelpersKt.setTypeReference(this, getValueParameterList(), typeRef);
    }

    @Nullable
    @Override
    public PsiElement getColon() {
        return findChildByType(KtTokens.COLON);
    }

    @Override
    public boolean isLocal() {
        PsiElement parent = getParent();
        return !(parent instanceof KtFile || parent instanceof KtClassBody);
    }

    public boolean isTopLevel() {
        KotlinFunctionStub stub = getStub();
        if (stub != null) {
            return stub.isTopLevel();
        }

        return getParent() instanceof KtFile;
    }

    @Override
    public boolean shouldChangeModificationCount(PsiElement place) {
        // Suppress Java check for out-of-block
        return false;
    }

    @Override
    public KtContractEffectList getContractDescription() {
        return findChildByType(KtNodeTypes.CONTRACT_EFFECT_LIST);
    }

    public boolean mayHaveContract() {
        return mayHaveContract(true);
    }

    public boolean mayHaveContract(boolean isAllowedOnMembers) {
        KotlinFunctionStub stub = getStub();
        if (stub != null) {
            return stub.mayHaveContract();
        }

        return KtPsiUtilKt.isContractPresentPsiCheck(this, isAllowedOnMembers);
    }
}
