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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.psi.stubs.KotlinPropertyAccessorStub;
import org.jetbrains.kotlin.psi.stubs.elements.JetStubElementTypes;
import org.jetbrains.kotlin.lexer.JetTokens;

import java.util.Collections;
import java.util.List;

public class JetPropertyAccessor extends JetDeclarationStub<KotlinPropertyAccessorStub>
        implements JetDeclarationWithBody, JetModifierListOwner, JetWithExpressionInitializer {
    public JetPropertyAccessor(@NotNull ASTNode node) {
        super(node);
    }

    public JetPropertyAccessor(@NotNull KotlinPropertyAccessorStub stub) {
        super(stub, JetStubElementTypes.PROPERTY_ACCESSOR);
    }

    @Override
    public <R, D> R accept(@NotNull JetVisitor<R, D> visitor, D data) {
        return visitor.visitPropertyAccessor(this, data);
    }

    public boolean isSetter() {
        KotlinPropertyAccessorStub stub = getStub();
        if (stub != null) {
            return !stub.isGetter();
        }
        return findChildByType(JetTokens.SET_KEYWORD) != null;
    }

    public boolean isGetter() {
        KotlinPropertyAccessorStub stub = getStub();
        if (stub != null) {
            return stub.isGetter();
        }
        return findChildByType(JetTokens.GET_KEYWORD) != null;
    }

    @Nullable
    public JetParameterList getParameterList() {
        return getStubOrPsiChild(JetStubElementTypes.VALUE_PARAMETER_LIST);
    }

    @Nullable
    public JetParameter getParameter() {
        JetParameterList parameterList = getParameterList();
        if (parameterList == null) return null;
        List<JetParameter> parameters = parameterList.getParameters();
        if (parameters.isEmpty()) return null;
        return parameters.get(0);
    }

    @NotNull
    @Override
    public List<JetParameter> getValueParameters() {
        JetParameter parameter = getParameter();
        if (parameter == null) {
            return Collections.emptyList();
        }
        return Collections.singletonList(parameter);
    }

    @Nullable
    @Override
    public JetExpression getBodyExpression() {
        return findChildByClass(JetExpression.class);
    }

    @Override
    public boolean hasBlockBody() {
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
        return findChildByType(JetTokens.EQ);
    }

    @Override
    public boolean hasDeclaredReturnType() {
        return true;
    }

    @Nullable
    public JetTypeReference getReturnTypeReference() {
        return getStubOrPsiChild(JetStubElementTypes.TYPE_REFERENCE);
    }

    @NotNull
    public PsiElement getNamePlaceholder() {
        PsiElement get = findChildByType(JetTokens.GET_KEYWORD);
        if (get != null) {
            return get;
        }
        return findChildByType(JetTokens.SET_KEYWORD);
    }

    @Nullable
    public ASTNode getRightParenthesis() {
        return getNode().findChildByType(JetTokens.RPAR);
    }

    @Nullable
    @Override
    public JetExpression getInitializer() {
        return PsiTreeUtil.getNextSiblingOfType(getEqualsToken(), JetExpression.class);
    }

    @Override
    public boolean hasInitializer() {
        return getInitializer() != null;
    }
}
