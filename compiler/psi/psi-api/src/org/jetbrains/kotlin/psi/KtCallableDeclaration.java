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

/**
 * Represents a declaration with the structure of a callable: it may declare value parameters, an extension receiver,
 * type parameters (with constraints), and a return type.
 *
 * <p>Its concrete forms are functions ({@link KtFunction}, which covers named functions, function literals, and
 * constructors), properties ({@link KtProperty}), and value parameters ({@link KtParameter}). Not every form supports
 * every element: for instance, a property has no value parameter list, and a constructor has no receiver.
 *
 * <h3>Example:</h3>
 * <pre>{@code
 *        fun <T> List<T>.second(index: Int): T = this[index]
 * //     ^_________________________________________________^
 * // Receiver 'List<T>', type parameter 'T', value parameter 'index', and return type 'T'
 * }</pre>
 *
 * @see KtFunction
 * @see KtProperty
 * @see KtParameter
 */
public interface KtCallableDeclaration extends KtNamedDeclaration, KtDeclarationWithReturnType, KtTypeParameterListOwner {
    /**
     * Returns the parenthesized list of value parameters, or {@code null} if this callable has none (for example, a
     * property).
     */
    @Nullable
    KtParameterList getValueParameterList();

    /**
     * Returns the value parameters of this callable, or an empty list if there are none.
     */
    @NotNull
    List<KtParameter> getValueParameters();

    /**
     * Returns the type reference of the extension receiver, or {@code null} if this callable is not an extension.
     *
     * <h3>Example:</h3>
     * <pre>{@code
     * fun String.trimAll(): String = trim()
     * //  ^____^
     * // The receiver type reference
     * }</pre>
     */
    @Nullable
    KtTypeReference getReceiverTypeReference();

    /**
     * Returns the context receivers declared in the {@code context(...)} clause of this callable, or an empty list if
     * there are none.
     *
     * @see KtContextReceiver
     * @see KtModifierList#getContextParameterList()
     */
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

        return contextParameterList.getContextParameters();
    }

    @Override
    @Nullable
    KtTypeReference getTypeReference();

    /**
     * @deprecated Use {@code org.jetbrains.kotlin.idea.base.psi.KotlinPsiModificationUtils.setCallableTypeReference(this, null, typeRef)}
     * instead.
     */
    @SuppressWarnings("unused") // used in Kotlin IDE plugin
    @Deprecated
    @Nullable
    KtTypeReference setTypeReference(@Nullable KtTypeReference typeRef);

    /**
     * Returns the colon token that separates the declaration from its return type, or {@code null} if the return type
     * is omitted.
     */
    @Nullable
    PsiElement getColon();
}
