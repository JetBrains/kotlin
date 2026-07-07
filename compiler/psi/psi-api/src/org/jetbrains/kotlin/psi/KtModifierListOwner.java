/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.psi;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken;

/**
 * Represents an element that may own a {@link KtModifierList}, that is, a sequence of modifiers (such as
 * {@code public}, {@code inline}, {@code suspend}) and annotations.
 *
 * <p>Since annotations are stored in the modifier list, this interface extends {@link KtAnnotated}. Most declarations
 * are modifier-list owners, and so are some non-declaration elements such as type references and value parameters.
 */
public interface KtModifierListOwner extends PsiElement, KtAnnotated {
    /**
     * Returns the modifier list of this element, or {@code null} if it carries neither modifiers nor annotations.
     */
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
    @Deprecated
    void addModifier(@NotNull KtModifierKeywordToken modifier);

    /**
     * @deprecated Use {@code org.jetbrains.kotlin.idea.base.psi.KotlinPsiModificationUtils.removeModifierKeyword(this, modifier)}
     * instead.
     */
    @Deprecated
    void removeModifier(@NotNull KtModifierKeywordToken modifier);

    /**
     * @deprecated Use {@code org.jetbrains.kotlin.idea.base.psi.KotlinPsiModificationUtils.addAnnotation(this, annotationEntry)}
     * instead.
     */
    @Deprecated
    @NotNull
    KtAnnotationEntry addAnnotationEntry(@NotNull KtAnnotationEntry annotationEntry);
}
