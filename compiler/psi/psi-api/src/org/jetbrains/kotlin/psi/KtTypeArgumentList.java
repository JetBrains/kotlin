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
import org.jetbrains.kotlin.psi.psiUtil.KtPsiUtilKt;
import org.jetbrains.kotlin.psi.stubs.KotlinPlaceHolderStub;
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes;

import java.util.List;

public class KtTypeArgumentList extends KtElementImplStub<KotlinPlaceHolderStub<KtTypeArgumentList>> {
    public KtTypeArgumentList(@NotNull ASTNode node) {
        super(node);
    }

    public KtTypeArgumentList(@NotNull KotlinPlaceHolderStub<KtTypeArgumentList> stub) {
        super(stub, KtStubElementTypes.TYPE_ARGUMENT_LIST);
    }

    @Override
    public <R, D> R accept(@NotNull KtVisitor<R, D> visitor, D data) {
        return visitor.visitTypeArgumentList(this, data);
    }

    @NotNull
    public List<KtTypeProjection> getArguments() {
        return getStubOrPsiChildrenAsList(KtStubElementTypes.TYPE_PROJECTION);
    }

    @NotNull
    public KtTypeProjection addArgument(@NotNull KtTypeProjection typeArgument) {
        return EditCommaSeparatedListHelper.INSTANCE.addItem(this, getArguments(), typeArgument, KtTokens.LT);
    }

    @Nullable
    public PsiElement getTrailingComma() {
        return KtPsiUtilKt.getTrailingCommaByClosingElement(findChildByType(KtTokens.GT));
    }
}
