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

/**
 * Represents the list of modifiers and annotations that precede a declaration or other {@link KtModifierListOwner}.
 *
 * <p>A modifier list groups plain modifier keywords (such as {@code public}, {@code inline}, {@code suspend}),
 * annotation entries, and the {@code context(...)} parameter list. Whether a specific modifier is present can be tested
 * with {@link #hasModifier(KtModifierKeywordToken)}.
 *
 * <h3>Example:</h3>
 * <pre>{@code
 *    @JvmStatic private inline fun foo() {}
 * // ^______________________ ^
 * // The modifier list ('@JvmStatic private inline')
 * }</pre>
 */
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
     * Returns the list of all {@link KtContextParameterList} declared in this modifier list.
     * <p>
     * This method is intended only for handling error cases since valid code cannot have more than one context parameter list.
     * <p>
     * Prefer {@link #getContextParameterList()} where it is possible.
     */
    @NotNull
    public List<KtContextParameterList> getContextParameterLists() {
        return getStubOrPsiChildrenAsList(KtStubBasedElementTypes.CONTEXT_PARAMETER_LIST);
    }

    /**
     * Returns the list of all {@link KtContextParameterList} declared in this modifier list.
     *
     * @deprecated Use {@link #getContextParameterLists()} instead. This method is obsolete and exists for compatibility reasons only.
     */
    @NotNull
    @Deprecated
    @SuppressWarnings("unchecked")
    public List<KtContextReceiverList> getContextReceiverLists() {
        return (List<KtContextReceiverList>)(List<?>) getContextParameterLists();
    }

    @Override
    @NotNull
    public List<KtAnnotationEntry> getAnnotationEntries() {
        return KtPsiUtilKt.collectAnnotationEntriesFromStubOrPsi(this);
    }

    /**
     * Returns {@code true} if this modifier list contains the given modifier keyword.
     */
    public boolean hasModifier(@NotNull KtModifierKeywordToken tokenType) {
        KotlinModifierListStub stub = getStub();
        if (stub != null) {
            return stub.hasModifier(tokenType);
        }
        return getModifier(tokenType) != null;
    }

    /**
     * Returns the token for the given modifier keyword, or {@code null} if this modifier list does not contain it.
     */
    @Nullable
    public PsiElement getModifier(@NotNull KtModifierKeywordToken tokenType) {
        return findChildByType(tokenType);
    }

    /**
     * Returns the first token whose type is in the given set, or {@code null} if none is present.
     */
    @Nullable
    public PsiElement getModifier(@NotNull TokenSet tokenTypes) {
        return findChildByType(tokenTypes);
    }


    /**
     * Returns the element that owns this modifier list (the declaration or other {@link KtModifierListOwner} it
     * belongs to).
     */
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
