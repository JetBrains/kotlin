/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi;

import com.intellij.lang.ASTNode;
import kotlin.ReplaceWith;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.KtNodeTypes;
import org.jetbrains.kotlin.kdoc.psi.api.KDoc;
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken;
import org.jetbrains.kotlin.psi.findDocComment.FindDocCommentKt;

import java.util.Collections;
import java.util.List;

/**
 * Base implementation of {@link KtDeclaration} backed directly by the AST tree.
 *
 * <p>This is an internal implementation base class of the Kotlin PSI, used by declaration types that are never represented by a stub. It is
 * not intended for direct use or subclassing outside of the PSI implementation. For declarations that may also be backed by a stub, see
 * {@link KtDeclarationStub}.
 */
public abstract class KtDeclarationImpl extends KtExpressionImpl implements KtDeclaration {
    @KtImplementationDetail
    public KtDeclarationImpl(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    @Nullable
    public KtModifierList getModifierList() {
        return (KtModifierList) findChildByType(KtNodeTypes.MODIFIER_LIST);
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

    @NotNull
    @Override
    public List<KtAnnotationEntry> getAnnotationEntries() {
        KtModifierList modifierList = getModifierList();
        if (modifierList == null) return Collections.emptyList();
        return modifierList.getAnnotationEntries();
    }

    @NotNull
    @Override
    public List<KtAnnotation> getAnnotations() {
        KtModifierList modifierList = getModifierList();
        if (modifierList == null) return Collections.emptyList();
        return modifierList.getAnnotations();
    }

    @Nullable
    @Override
    public KDoc getDocComment() {
        return FindDocCommentKt.findDocComment(this);
    }
}
