/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.KtStubBasedElementTypes;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.psi.psiUtil.KtPsiUtilKt;
import org.jetbrains.kotlin.psi.stubs.KotlinPlaceHolderStub;

import java.util.List;

/**
 * Represents a list of type parameters in angle brackets.
 *
 * <h3>Example:</h3>
 * <pre>{@code
 * class Box<T, U>(val first: T, val second: U)
 * //       ^____^
 * }</pre>
 */
public class KtTypeParameterList extends KtElementImplStub<KotlinPlaceHolderStub<KtTypeParameterList>> {
    public KtTypeParameterList(@NotNull ASTNode node) {
        super(node);
    }

    public KtTypeParameterList(@NotNull KotlinPlaceHolderStub<KtTypeParameterList> stub) {
        super(stub, KtStubBasedElementTypes.TYPE_PARAMETER_LIST);
    }

    @NotNull
    public List<KtTypeParameter> getParameters() {
        return getStubOrPsiChildrenAsList(KtStubBasedElementTypes.TYPE_PARAMETER);
    }

    @NotNull
    public KtTypeParameter addParameter(@NotNull KtTypeParameter typeParameter) {
        return EditCommaSeparatedListHelper.INSTANCE.addItem(this, getParameters(), typeParameter, KtTokens.LT);
    }

    @Override
    public <R, D> R accept(@NotNull KtVisitor<R, D> visitor, D data) {
        return visitor.visitTypeParameterList(this, data);
    }

    @Nullable
    public PsiElement getTrailingComma() {
        return KtPsiUtilKt.getTrailingCommaByClosingElement(findChildByType(KtTokens.GT));
    }
}
