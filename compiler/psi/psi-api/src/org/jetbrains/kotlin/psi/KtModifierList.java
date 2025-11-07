/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.KtStubBasedElementTypes;
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken;
import org.jetbrains.kotlin.psi.psiUtil.KtPsiUtilKt;
import org.jetbrains.kotlin.psi.stubs.KotlinModifierListStub;

import java.util.List;

public abstract class KtModifierList extends KtElementImplStub<KotlinModifierListStub> implements KtAnnotationsContainer {

    public KtModifierList(@NotNull KotlinModifierListStub stub, @NotNull IStubElementType nodeType) {
        super(stub, nodeType);
    }

    public KtModifierList(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public <R, D> R accept(@NotNull KtVisitor<R, D> visitor, D data) {
        return visitor.visitModifierList(this, data);
    }

    @Override
    @NotNull
    public List<KtAnnotation> getAnnotations() {
        return getStubOrPsiChildrenAsList(KtStubBasedElementTypes.ANNOTATION);
    }

    /**
     * Returns the context parameter list for this modifier list, if present.
     *
     * <p><b>Example:</b></p>
     * <pre>{@code
     * context(c: Context)
     * fun foo() {}
     * }</pre>
     *
     * @return the context parameter list, or {@code null} if this modifier list has no context parameters
     *
     * @see KtContextParameterList
     */
    @Nullable
    @SuppressWarnings("deprecation") // KT-78356
    public KtContextParameterList getContextParameterList() {
        return getStubOrPsiChild(KtStubBasedElementTypes.CONTEXT_PARAMETER_LIST);
    }

    /**
     * Returns the context receiver list for this modifier list, if present.
     *
     * @return the context receiver list, or {@code null} if this modifier list has no context receivers
     * @deprecated Use {@link #getContextParameterList()} instead. This method is obsolete and exists for compatibility reasons only.
     */
    @Deprecated
    @Nullable
    public KtContextReceiverList getContextReceiverList() {
        return (KtContextReceiverList) getContextParameterList();
    }

    /**
     * Experimental context parameter API.
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public List<KtContextReceiverList> getContextReceiverLists() {
        return (List<KtContextReceiverList>)(List<?>) getStubOrPsiChildrenAsList(KtStubBasedElementTypes.CONTEXT_PARAMETER_LIST);
    }

    @Override
    @NotNull
    public List<KtAnnotationEntry> getAnnotationEntries() {
        return KtPsiUtilKt.collectAnnotationEntriesFromStubOrPsi(this);
    }

    public boolean hasModifier(@NotNull KtModifierKeywordToken tokenType) {
        KotlinModifierListStub stub = getStub();
        if (stub != null) {
            return stub.hasModifier(tokenType);
        }
        return getModifier(tokenType) != null;
    }

    @Nullable
    public PsiElement getModifier(@NotNull KtModifierKeywordToken tokenType) {
        return findChildByType(tokenType);
    }

    @Nullable
    public PsiElement getModifier(@NotNull TokenSet tokenTypes) {
        return findChildByType(tokenTypes);
    }


    public PsiElement getOwner() {
        return getParentByStub();
    }

    @Override
    public void deleteChildInternal(@NotNull ASTNode child) {
        super.deleteChildInternal(child);
        if (getFirstChild() == null) {
            delete();
        }
    }
}
