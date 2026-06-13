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

    /**
     * @deprecated Use {@code org.jetbrains.kotlin.idea.base.psi.KotlinPsiModificationUtils.appendParameter(this, parameter)}
     * instead.
     */
    @NotNull
    @Deprecated
    public KtParameter addParameter(@NotNull KtParameter parameter) {
        return KtPsiMutationService.getInstance().appendParameter(this, parameter);
    }

    /**
     * @deprecated Use {@code org.jetbrains.kotlin.idea.base.psi.KotlinPsiModificationUtils.insertParameterBefore(this, parameter, anchor)}
     * instead.
     */
    @NotNull
    @Deprecated
    public KtParameter addParameterBefore(@NotNull KtParameter parameter, @Nullable KtParameter anchor) {
        return KtPsiMutationService.getInstance().insertParameterBefore(this, parameter, anchor);
    }

    /**
     * @deprecated Use {@code org.jetbrains.kotlin.idea.base.psi.KotlinPsiModificationUtils.insertParameterAfter(this, parameter, anchor)}
     * instead.
     */
    @NotNull
    @Deprecated
    public KtParameter addParameterAfter(@NotNull KtParameter parameter, @Nullable KtParameter anchor) {
        return KtPsiMutationService.getInstance().insertParameterAfter(this, parameter, anchor);
    }

    /**
     * @deprecated Use {@code org.jetbrains.kotlin.idea.base.psi.KotlinPsiModificationUtils.deleteParameter(this, parameter)}
     * instead.
     */
    @Deprecated
    public void removeParameter(@NotNull KtParameter parameter) {
        KtPsiMutationService.getInstance().deleteParameter(this, parameter);
    }

    /**
     * @deprecated Use {@code org.jetbrains.kotlin.idea.base.psi.KotlinPsiModificationUtils.deleteParameter(this, index)}
     * instead.
     */
    @Deprecated
    public void removeParameter(int index) {
        KtPsiMutationService.getInstance().deleteParameter(this, index);
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
