/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public interface KtCallableDeclaration extends KtNamedDeclaration, KtDeclarationWithReturnType, KtTypeParameterListOwner {
    @Nullable
    KtParameterList getValueParameterList();

    @NotNull
    List<KtParameter> getValueParameters();

    @Nullable
    KtTypeReference getReceiverTypeReference();

    @NotNull
    default List<KtContextReceiver> getContextReceivers() {
        KtModifierList modifierList = getModifierList();
        if (modifierList == null) {
            return Collections.emptyList();
        }

        KtContextParameterList contextParameterList = modifierList.getContextParameterList();
        if (contextParameterList == null) {
            return Collections.emptyList();
        }

        return contextParameterList.contextReceivers();
    }

    /**
     * Returns the context parameters declared in this callable declaration.
     * <p>
     * Context parameters are declared using the {@code context(...)} syntax in the modifiers section
     * of a callable declaration. For example:
     * <pre>
     * context(logger: Logger, config: Config)
     * fun processData() { ... }
     * </pre>
     *
     * @return a non-null list of {@link KtParameter} representing the context parameters.
     *         Returns an empty list if no context parameters are declared.
     *
     * @see KtContextParameterList
     * @see KtModifierList#getContextParameterList()
     */
    @NotNull
    default List<KtParameter> getContextParameters() {
        KtModifierList modifierList = getModifierList();
        if (modifierList == null) {
            return Collections.emptyList();
        }

        KtContextParameterList contextParameterList = modifierList.getContextParameterList();
        if (contextParameterList == null) {
            return Collections.emptyList();
        }

        return contextParameterList.contextParameters();
    }

    @Override
    @Nullable
    KtTypeReference getTypeReference();

    @SuppressWarnings("unused") // used in Kotlin IDE plugin
    @Nullable
    KtTypeReference setTypeReference(@Nullable KtTypeReference typeRef);

    @Nullable
    PsiElement getColon();
}
