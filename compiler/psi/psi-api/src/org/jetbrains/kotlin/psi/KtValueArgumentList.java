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
 * Represents a parenthesized list of value arguments in a call.
 *
 * <h3>Example:</h3>
 * <pre>{@code
 * println("Hello", "World")
 * //     ^________________^
 * }</pre>
 */
public class KtValueArgumentList extends KtElementImplStub<KotlinPlaceHolderStub<KtValueArgumentList>> {
    public KtValueArgumentList(@NotNull ASTNode node) {
        super(node);
    }

    public KtValueArgumentList(@NotNull KotlinPlaceHolderStub<KtValueArgumentList> stub) {
        super(stub, KtStubBasedElementTypes.VALUE_ARGUMENT_LIST);
    }

    @Override
    public <R, D> R accept(@NotNull KtVisitor<R, D> visitor, D data) {
        return visitor.visitValueArgumentList(this, data);
    }

    @NotNull
    public List<KtValueArgument> getArguments() {
        return getStubOrPsiChildrenAsList(KtStubBasedElementTypes.VALUE_ARGUMENT);
    }

    @Nullable
    public PsiElement getRightParenthesis() {
        return findChildByType(KtTokens.RPAR);
    }

    @Nullable
    public PsiElement getLeftParenthesis() {
        return findChildByType(KtTokens.LPAR);
    }

    /**
     * @deprecated Use {@code org.jetbrains.kotlin.idea.base.psi.KotlinPsiModificationUtils.appendValueArgument(this, argument)}
     * instead.
     */
    @NotNull
    @Deprecated
    public KtValueArgument addArgument(@NotNull KtValueArgument argument) {
        return KtPsiMutationService.getInstance().appendValueArgument(this, argument);
    }

    /**
     * @deprecated Use {@code org.jetbrains.kotlin.idea.base.psi.KotlinPsiModificationUtils.insertValueArgumentAfter(this, argument, anchor)}
     * instead.
     */
    @NotNull
    @Deprecated
    public KtValueArgument addArgumentAfter(@NotNull KtValueArgument argument, @Nullable KtValueArgument anchor) {
        return KtPsiMutationService.getInstance().insertValueArgumentAfter(this, argument, anchor);
    }

    /**
     * @deprecated Use {@code org.jetbrains.kotlin.idea.base.psi.KotlinPsiModificationUtils.insertValueArgumentBefore(this, argument, anchor)}
     * instead.
     */
    @NotNull
    @Deprecated
    public KtValueArgument addArgumentBefore(@NotNull KtValueArgument argument, @Nullable KtValueArgument anchor) {
        return KtPsiMutationService.getInstance().insertValueArgumentBefore(this, argument, anchor);
    }

    /**
     * @deprecated Use {@code org.jetbrains.kotlin.idea.base.psi.KotlinPsiModificationUtils.deleteValueArgument(this, argument)}
     * instead.
     */
    @Deprecated
    public void removeArgument(@NotNull KtValueArgument argument) {
        KtPsiMutationService.getInstance().deleteValueArgument(this, argument);
    }

    /**
     * @deprecated Use {@code org.jetbrains.kotlin.idea.base.psi.KotlinPsiModificationUtils.deleteValueArgument(this, index)}
     * instead.
     */
    @Deprecated
    public void removeArgument(int index) {
        KtPsiMutationService.getInstance().deleteValueArgument(this, index);
    }

    public PsiElement getTrailingComma() {
        return KtPsiUtilKt.getTrailingCommaByClosingElement(getRightParenthesis());
    }
}
