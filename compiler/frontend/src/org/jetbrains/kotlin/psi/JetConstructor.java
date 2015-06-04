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
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.lexer.JetTokens;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.psi.stubs.KotlinPlaceHolderStub;
import org.jetbrains.kotlin.psi.stubs.elements.JetStubElementTypes;

import java.util.Collections;
import java.util.List;

public abstract class JetConstructor<T extends JetConstructor<T>> extends JetDeclarationStub<KotlinPlaceHolderStub<T>> implements JetFunction {
    protected JetConstructor(@NotNull ASTNode node) {
        super(node);
    }

    protected JetConstructor(@NotNull KotlinPlaceHolderStub<T> stub, @NotNull IStubElementType nodeType) {
        super(stub, nodeType);
    }

    @NotNull
    public abstract JetClassOrObject getClassOrObject();

    @Override
    public boolean isLocal() {
        return false;
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

    @Nullable
    @Override
    public JetTypeReference getReceiverTypeReference() {
        return null;
    }

    @Nullable
    @Override
    public JetTypeReference getTypeReference() {
        return null;
    }

    @Nullable
    @Override
    public JetTypeReference setTypeReference(@Nullable JetTypeReference typeRef) {
        return null;
    }

    @Nullable
    @Override
    public PsiElement getColon() {
        return findChildByType(JetTokens.COLON);
    }

    @Nullable
    @Override
    public JetBlockExpression getBodyExpression() {
        return null;
    }

    @Nullable
    @Override
    public PsiElement getEqualsToken() {
        return null;
    }

    @Override
    public boolean hasBlockBody() {
        return true;
    }

    @Override
    public boolean hasBody() {
        return getBodyExpression() != null;
    }

    @Override
    public boolean hasDeclaredReturnType() {
        return false;
    }

    @Nullable
    @Override
    public JetTypeParameterList getTypeParameterList() {
        return null;
    }

    @Nullable
    @Override
    public JetTypeConstraintList getTypeConstraintList() {
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

    @Override
    @NotNull
    public String getName() {
        return getClassOrObject().getName();
    }

    @NotNull
    @Override
    public Name getNameAsSafeName() {
        return Name.identifier(getName());
    }

    @Nullable
    @Override
    public FqName getFqName() {
        return null;
    }

    @Nullable
    @Override
    public Name getNameAsName() {
        return getNameAsSafeName();
    }

    @Nullable
    @Override
    public PsiElement getNameIdentifier() {
        return null;
    }

    @Override
    public PsiElement setName(@NotNull String name) throws IncorrectOperationException {
        throw new IncorrectOperationException("setName to constructor");
    }

    @Nullable
    public PsiElement getConstructorKeyword() {
        return findChildByType(JetTokens.CONSTRUCTOR_KEYWORD);
    }

    public boolean hasConstructorKeyword() {
        if (getStub() != null) return true;
        return getConstructorKeyword() != null;
    }

    @Override
    public int getTextOffset() {
        PsiElement keyword = getConstructorKeyword();
        if (keyword != null) return keyword.getTextOffset();

        JetParameterList parameterList = getValueParameterList();
        if (parameterList != null) return parameterList.getTextOffset();

        return super.getTextOffset();
    }
}
