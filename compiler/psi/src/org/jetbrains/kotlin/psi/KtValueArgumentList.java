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

public class KtValueArgumentList extends KtElementImplStub<KotlinPlaceHolderStub<KtValueArgumentList>> {
    public KtValueArgumentList(@NotNull ASTNode node) {
        super(node);
    }

    public KtValueArgumentList(@NotNull KotlinPlaceHolderStub<KtValueArgumentList> stub) {
        super(stub, KtStubElementTypes.VALUE_ARGUMENT_LIST);
    }

    @Override
    public <R, D> R accept(@NotNull KtVisitor<R, D> visitor, D data) {
        return visitor.visitValueArgumentList(this, data);
    }

    @NotNull
    public List<KtValueArgument> getArguments() {
        return getStubOrPsiChildrenAsList(KtStubElementTypes.VALUE_ARGUMENT);
    }

    @Nullable
    public PsiElement getRightParenthesis() {
        return findChildByType(KtTokens.RPAR);
    }

    @Nullable
    public PsiElement getLeftParenthesis() {
        return findChildByType(KtTokens.LPAR);
    }

    @NotNull
    public KtValueArgument addArgument(@NotNull KtValueArgument argument) {
        return EditCommaSeparatedListHelper.INSTANCE.addItem(this, getArguments(), argument);
    }

    @NotNull
    public KtValueArgument addArgumentAfter(@NotNull KtValueArgument argument, @Nullable KtValueArgument anchor) {
        return EditCommaSeparatedListHelper.INSTANCE.addItemAfter(this, getArguments(), argument, anchor);
    }

    @NotNull
    public KtValueArgument addArgumentBefore(@NotNull KtValueArgument argument, @Nullable KtValueArgument anchor) {
        return EditCommaSeparatedListHelper.INSTANCE.addItemBefore(this, getArguments(), argument, anchor);
    }

    public void removeArgument(@NotNull KtValueArgument argument) {
        assert argument.getParent() == this;
        EditCommaSeparatedListHelper.INSTANCE.removeItem(argument);
    }

    public void removeArgument(int index) {
        removeArgument(getArguments().get(index));
    }
}
