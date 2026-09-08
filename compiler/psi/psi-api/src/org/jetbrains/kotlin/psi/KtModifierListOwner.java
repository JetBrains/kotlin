/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi;

import com.intellij.psi.PsiElement;
import kotlin.ReplaceWith;
import kotlin.SubclassOptInRequired;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken;

/**
 * Represents an element that may own a {@link KtModifierList}, that is, a sequence of modifiers (such as {@code public}, {@code inline},
 * {@code suspend}) and annotations.
 *
 * <p>Since annotations are stored in the modifier list, this interface extends {@link KtAnnotated}. Most declarations are modifier-list
 * owners, and so are some non-declaration elements such as type references and value parameters.
 */
@SubclassOptInRequired(markerClass = KtImplementationDetail.class)
public interface KtModifierListOwner extends PsiElement, KtAnnotated {
    /** Returns the modifier list of this element, or {@code null} if it carries neither modifiers nor annotations. */
    @Nullable
    KtModifierList getModifierList();

    /**
     * Returns {@code true} if this element declares the given modifier keyword (for example,
     * {@link org.jetbrains.kotlin.lexer.KtTokens#PRIVATE_KEYWORD}).
     */
    boolean hasModifier(@NotNull KtModifierKeywordToken modifier);

    /**
     * @deprecated Use {@code org.jetbrains.kotlin.idea.base.psi.KotlinPsiModificationUtils.addModifierKeyword(this, modifier)}
     * instead.
     */
    @kotlin.Deprecated(
            message = "Use 'org.jetbrains.kotlin.idea.base.psi.KotlinPsiModificationUtils.addModifierKeyword(this, modifier)' instead.",
            replaceWith = @ReplaceWith(
                    expression = "this.addModifierKeyword(modifier)",
                    imports = "org.jetbrains.kotlin.idea.base.psi.addModifierKeyword"
            )
    )
    @Deprecated
    void addModifier(@NotNull KtModifierKeywordToken modifier);

    /**
     * @deprecated Use {@code org.jetbrains.kotlin.idea.base.psi.KotlinPsiModificationUtils.removeModifierKeyword(this, modifier)}
     * instead.
     */
    @kotlin.Deprecated(
            message = "Use 'org.jetbrains.kotlin.idea.base.psi.KotlinPsiModificationUtils.removeModifierKeyword(this, modifier)' instead.",
            replaceWith = @ReplaceWith(
                    expression = "this.removeModifierKeyword(modifier)",
                    imports = "org.jetbrains.kotlin.idea.base.psi.removeModifierKeyword"
            )
    )
    @Deprecated
    void removeModifier(@NotNull KtModifierKeywordToken modifier);

    /**
     * @deprecated Use {@code org.jetbrains.kotlin.idea.base.psi.KotlinPsiModificationUtils.addAnnotation(this, annotationEntry)}
     * instead.
     */
    @kotlin.Deprecated(
            message = "Use 'org.jetbrains.kotlin.idea.base.psi.KotlinPsiModificationUtils.addAnnotation(this, annotationEntry)' instead.",
            replaceWith = @ReplaceWith(
                    expression = "this.addAnnotation(annotationEntry)",
                    imports = "org.jetbrains.kotlin.idea.base.psi.addAnnotation"
            )
    )
    @Deprecated
    @NotNull
    KtAnnotationEntry addAnnotationEntry(@NotNull KtAnnotationEntry annotationEntry);
}
