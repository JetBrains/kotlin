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
 * Represents a list of parameters in various contexts.
 *
 * <h3>Function parameters (including constructors and property accessors):</h3>
 * <pre>{@code
 * fun greet(name: String, age: Int) {}
 * //       ^______________________^
 * }</pre>
 *
 * <h3>Lambda parameters:</h3>
 * <pre>{@code
 * val f = { name: String, age: Int -> name }
 * //        ^____________________^
 * }</pre>
 *
 * <h3>Function type parameters:</h3>
 * <pre>{@code
 * val f: (String, Int) -> Unit = { _, _ -> }
 * //     ^___________^
 * }</pre>
 *
 * @see KtParameter
 * @see #getOwnerFunction()
 */
public class KtParameterList extends KtElementImplStub<KotlinPlaceHolderStub<KtParameterList>> {
    public KtParameterList(@NotNull ASTNode node) {
        super(node);
    }

    public KtParameterList(@NotNull KotlinPlaceHolderStub<KtParameterList> stub) {
        super(stub, KtStubBasedElementTypes.VALUE_PARAMETER_LIST);
    }

    @Override
    public <R, D> R accept(@NotNull KtVisitor<R, D> visitor, D data) {
        return visitor.visitParameterList(this, data);
    }

    @NotNull
    public List<KtParameter> getParameters() {
        return getStubOrPsiChildrenAsList(KtStubBasedElementTypes.VALUE_PARAMETER);
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

    public KtDeclarationWithBody getOwnerFunction() {
        PsiElement parent = getParentByStub();
        if (!(parent instanceof KtDeclarationWithBody)) return null;
        return (KtDeclarationWithBody) parent;
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
    public PsiElement getFirstComma() {
        return findChildByType(KtTokens.COMMA);
    }

    @Nullable
    public PsiElement getTrailingComma() {
        PsiElement parentElement = getParent();
        if (parentElement instanceof KtFunctionLiteral) {
            return KtPsiUtilKt.getTrailingCommaByElementsList(this);
        } else {
            return KtPsiUtilKt.getTrailingCommaByClosingElement(getRightParenthesis());
        }
    }
}
