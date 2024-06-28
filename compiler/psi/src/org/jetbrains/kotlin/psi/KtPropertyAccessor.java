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
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.AstLoadingFilter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.KtNodeTypes;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.psi.stubs.KotlinPropertyAccessorStub;
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes;

import java.util.Collections;
import java.util.List;

public class KtPropertyAccessor extends KtDeclarationStub<KotlinPropertyAccessorStub>
        implements KtDeclarationWithBody, KtModifierListOwner, KtDeclarationWithInitializer {
    public KtPropertyAccessor(@NotNull ASTNode node) {
        super(node);
    }

    public KtPropertyAccessor(@NotNull KotlinPropertyAccessorStub stub) {
        super(stub, KtStubElementTypes.PROPERTY_ACCESSOR);
    }

    @Override
    public <R, D> R accept(@NotNull KtVisitor<R, D> visitor, D data) {
        return visitor.visitPropertyAccessor(this, data);
    }

    public boolean isSetter() {
        KotlinPropertyAccessorStub stub = getStub();
        if (stub != null) {
            return !stub.isGetter();
        }
        return findChildByType(KtTokens.SET_KEYWORD) != null;
    }

    public boolean isGetter() {
        KotlinPropertyAccessorStub stub = getStub();
        if (stub != null) {
            return stub.isGetter();
        }
        return findChildByType(KtTokens.GET_KEYWORD) != null;
    }

    @Nullable
    public KtParameterList getParameterList() {
        return getStubOrPsiChild(KtStubElementTypes.VALUE_PARAMETER_LIST);
    }

    @Nullable
    public KtParameter getParameter() {
        KtParameterList parameterList = getParameterList();
        if (parameterList == null) return null;
        List<KtParameter> parameters = parameterList.getParameters();
        if (parameters.isEmpty()) return null;
        return parameters.get(0);
    }

    @NotNull
    @Override
    public List<KtParameter> getValueParameters() {
        KtParameter parameter = getParameter();
        if (parameter == null) {
            return Collections.emptyList();
        }
        return Collections.singletonList(parameter);
    }

    @Nullable
    @Override
    public KtExpression getBodyExpression() {
        KotlinPropertyAccessorStub stub = getStub();
        if (stub != null) {
            if (!stub.hasBody()) {
                return null;
            }

            if (getContainingKtFile().isCompiled()) {
                return null;
            }
        }

        return  findChildByClass(KtExpression.class);
    }

    @Nullable
    @Override
    public KtBlockExpression getBodyBlockExpression() {
        KotlinPropertyAccessorStub stub = getStub();
        if (stub != null) {
            if (!(stub.hasBlockBody() && stub.hasBody())) {
                return null;
            }
            if (getContainingKtFile().isCompiled()) {
                return null;
            }
        }

        KtExpression bodyExpression = findChildByClass(KtExpression.class);
        if (bodyExpression instanceof KtBlockExpression) {
            return (KtBlockExpression) bodyExpression;
        }

        return null;
    }

    @Override
    public boolean hasBlockBody() {
        KotlinPropertyAccessorStub stub = getStub();
        if (stub != null) {
            return stub.hasBlockBody();
        }
        return getEqualsToken() == null;
    }

    @Override
    public boolean hasBody() {
        KotlinPropertyAccessorStub stub = getStub();
        if (stub != null) {
            return stub.hasBody();
        }
        return getBodyExpression() != null;
    }

    @Override
    @Nullable
    public PsiElement getEqualsToken() {
        return findChildByType(KtTokens.EQ);
    }

    @Override
    public KtContractEffectList getContractDescription() {
        return getStubOrPsiChild(KtStubElementTypes.CONTRACT_EFFECT_LIST);
    }

    @Override
    public boolean hasDeclaredReturnType() {
        return true;
    }

    @Nullable
    public KtTypeReference getReturnTypeReference() {
        return getStubOrPsiChild(KtStubElementTypes.TYPE_REFERENCE);
    }

    @NotNull
    public PsiElement getNamePlaceholder() {
        PsiElement get = findChildByType(KtTokens.GET_KEYWORD);
        if (get != null) {
            return get;
        }
        return findChildByType(KtTokens.SET_KEYWORD);
    }

    @Nullable
    public PsiElement getRightParenthesis() {
        return findChildByType(KtTokens.RPAR);
    }

    @Nullable
    public PsiElement getLeftParenthesis() {
        return findChildByType(KtTokens.LPAR);
    }

    @Nullable
    @Override
    public KtExpression getInitializer() {
        return PsiTreeUtil.getNextSiblingOfType(getEqualsToken(), KtExpression.class);
    }

    @Override
    public boolean hasInitializer() {
        return getInitializer() != null;
    }

    @NotNull
    public KtProperty getProperty() {
        return (KtProperty) getParent();
    }

    @Override
    public int getTextOffset() {
        return getNamePlaceholder().getTextRange().getStartOffset();
    }
}
