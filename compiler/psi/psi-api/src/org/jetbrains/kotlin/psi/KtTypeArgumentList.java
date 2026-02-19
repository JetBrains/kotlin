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
 * Represents a list of type arguments in angle brackets.
 *
 * <h3>Example:</h3>
 * <pre>{@code
 * val pair: Pair<String, Int> = "str" to 1
 * //            ^___________^
 * }</pre>
 */
public class KtTypeArgumentList extends KtElementImplStub<KotlinPlaceHolderStub<KtTypeArgumentList>> {
    public KtTypeArgumentList(@NotNull ASTNode node) {
        super(node);
    }

    public KtTypeArgumentList(@NotNull KotlinPlaceHolderStub<KtTypeArgumentList> stub) {
        super(stub, KtStubBasedElementTypes.TYPE_ARGUMENT_LIST);
    }

    @Override
    public <R, D> R accept(@NotNull KtVisitor<R, D> visitor, D data) {
        return visitor.visitTypeArgumentList(this, data);
    }

    @NotNull
    public List<KtTypeProjection> getArguments() {
        return getStubOrPsiChildrenAsList(KtStubBasedElementTypes.TYPE_PROJECTION);
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
