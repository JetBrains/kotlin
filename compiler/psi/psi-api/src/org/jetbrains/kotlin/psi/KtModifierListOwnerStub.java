/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.tree.IElementType;
import kotlin.ReplaceWith;
import kotlin.SubclassOptInRequired;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.KtStubBasedElementTypes;
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken;

import java.util.Collections;
import java.util.List;

/**
 * Base implementation of {@link KtModifierListOwner} that may be backed either by the AST tree or by a stub.
 *
 * <p>This is an internal implementation base class of the Kotlin PSI, not intended for direct use or subclassing outside of the PSI
 * implementation. See {@link KtElementImplStub} for details on stub backing.
 *
 * @param <T> the type of stub backing this element
 */
@SubclassOptInRequired(markerClass = KtImplementationDetail.class)
public class KtModifierListOwnerStub<T extends StubElement<?>> extends KtElementImplStub<T> implements KtModifierListOwner {
    @KtImplementationDetail
    public KtModifierListOwnerStub(@NotNull ASTNode node) {
        super(node);
    }

    @KtImplementationDetail
    public KtModifierListOwnerStub(@NotNull T stub, @NotNull IElementType nodeType) {
        super(stub, nodeType);
    }

    @Override
    @Nullable
    @SuppressWarnings("deprecation") // KT-78356
    public KtModifierList getModifierList() {
        return getStubOrPsiChild(KtStubBasedElementTypes.MODIFIER_LIST);
    }

    /**
     * @return every modifier list directly under this element, in source order.
     *
     * <p>An element normally has at most one modifier list, and {@link #getModifierList()} is the accessor to use.
     * There are two situations where a second one shows up, and only then is this method needed.
     *
     * <p>A parenthesized type keeps the modifiers written inside the parentheses on the enclosing
     * {@link KtTypeReference}, because the parentheses themselves are not a PSI element:
     *
     * <pre>{@code
     * val x: @Foo (suspend () -> Unit) = {}
     * //     ^__^  ^_____^
     * }</pre>
     *
     * <p>Code that does not parse cleanly may also leave several modifier lists on one element.
     */
    @NotNull
    public List<KtDeclarationModifierList> getAllModifierLists() {
        return getStubOrPsiChildrenAsList(KtStubBasedElementTypes.MODIFIER_LIST);
    }

    @Override
    public boolean hasModifier(@NotNull KtModifierKeywordToken modifier) {
        KtModifierList modifierList = getModifierList();
        return modifierList != null && modifierList.hasModifier(modifier);
    }

    /**
     * @deprecated Use {@code org.jetbrains.kotlin.idea.base.psi.KotlinPsiModificationUtils.addModifierKeyword(this, modifier)}
     * instead.
     */
    @Override
    @kotlin.Deprecated(
            message = "Use 'org.jetbrains.kotlin.idea.base.psi.KotlinPsiModificationUtils.addModifierKeyword(this, modifier)' instead.",
            replaceWith = @ReplaceWith(
                    expression = "this.addModifierKeyword(modifier)",
                    imports = "org.jetbrains.kotlin.idea.base.psi.addModifierKeyword"
            )
    )
    @Deprecated
    public void addModifier(@NotNull KtModifierKeywordToken modifier) {
        KtPsiMutationService.getInstance().addModifierKeyword(this, modifier);
    }

    /**
     * @deprecated Use {@code org.jetbrains.kotlin.idea.base.psi.KotlinPsiModificationUtils.removeModifierKeyword(this, modifier)}
     * instead.
     */
    @Override
    @kotlin.Deprecated(
            message = "Use 'org.jetbrains.kotlin.idea.base.psi.KotlinPsiModificationUtils.removeModifierKeyword(this, modifier)' instead.",
            replaceWith = @ReplaceWith(
                    expression = "this.removeModifierKeyword(modifier)",
                    imports = "org.jetbrains.kotlin.idea.base.psi.removeModifierKeyword"
            )
    )
    @Deprecated
    public void removeModifier(@NotNull KtModifierKeywordToken modifier) {
        KtPsiMutationService.getInstance().removeModifierKeyword(this, modifier);
    }

    /**
     * @deprecated Use {@code org.jetbrains.kotlin.idea.base.psi.KotlinPsiModificationUtils.addAnnotation(this, annotationEntry)}
     * instead.
     */
    @NotNull
    @Override
    @kotlin.Deprecated(
            message = "Use 'org.jetbrains.kotlin.idea.base.psi.KotlinPsiModificationUtils.addAnnotation(this, annotationEntry)' instead.",
            replaceWith = @ReplaceWith(
                    expression = "this.addAnnotation(annotationEntry)",
                    imports = "org.jetbrains.kotlin.idea.base.psi.addAnnotation"
            )
    )
    @Deprecated
    public KtAnnotationEntry addAnnotationEntry(@NotNull KtAnnotationEntry annotationEntry) {
        return KtPsiMutationService.getInstance().addAnnotation(this, annotationEntry);
    }

    @Override
    @NotNull
    public List<KtAnnotationEntry> getAnnotationEntries() {
        KtModifierList modifierList = getModifierList();
        if (modifierList == null) return Collections.emptyList();
        return modifierList.getAnnotationEntries();
    }

    @Override
    @NotNull
    public List<KtAnnotation> getAnnotations() {
        KtModifierList modifierList = getModifierList();
        if (modifierList == null) return Collections.emptyList();
        return modifierList.getAnnotations();
    }
}
