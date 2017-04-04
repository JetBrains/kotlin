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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.psi.stubs.KotlinPlaceHolderStub;
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes;

import java.util.List;

public class KtParameterList extends KtElementImplStub<KotlinPlaceHolderStub<KtParameterList>> {
    public KtParameterList(@NotNull ASTNode node) {
        super(node);
    }

    public KtParameterList(@NotNull KotlinPlaceHolderStub<KtParameterList> stub) {
        super(stub, KtStubElementTypes.VALUE_PARAMETER_LIST);
    }

    @Override
    public <R, D> R accept(@NotNull KtVisitor<R, D> visitor, D data) {
        return visitor.visitParameterList(this, data);
    }

    @Override
    public PsiElement getParent() {
        KotlinPlaceHolderStub<KtParameterList> stub = getStub();
        return stub != null ? stub.getParentStub().getPsi() : super.getParent();
    }

    @NotNull
    public List<KtParameter> getParameters() {
        return getStubOrPsiChildrenAsList(KtStubElementTypes.VALUE_PARAMETER);
    }

    @NotNull
    public KtParameter addParameter(@NotNull KtParameter parameter) {
        return EditCommaSeparatedListHelper.INSTANCE.addItem(this, getParameters(), parameter);
    }

    @NotNull
    public KtParameter addParameterBefore(@NotNull KtParameter parameter, @Nullable KtParameter anchor) {
        return EditCommaSeparatedListHelper.INSTANCE.addItemBefore(this, getParameters(), parameter, anchor);
    }

    @NotNull
    public KtParameter addParameterAfter(@NotNull KtParameter parameter, @Nullable KtParameter anchor) {
        return EditCommaSeparatedListHelper.INSTANCE.addItemAfter(this, getParameters(), parameter, anchor);
    }

    public void removeParameter(@NotNull KtParameter parameter) {
        EditCommaSeparatedListHelper.INSTANCE.removeItem(parameter);
    }

    public void removeParameter(int index) {
        removeParameter(getParameters().get(index));
    }

    public KtFunction getOwnerFunction() {
        PsiElement parent = getParentByStub();
        if (!(parent instanceof KtFunction)) return null;
        return (KtFunction) parent;
    }

    @Nullable
    public PsiElement getRightParenthesis() {
        return findChildByType(KtTokens.RPAR);
    }

    @Nullable
    public PsiElement getLeftParenthesis() {
        return findChildByType(KtTokens.LPAR);
    }
}
