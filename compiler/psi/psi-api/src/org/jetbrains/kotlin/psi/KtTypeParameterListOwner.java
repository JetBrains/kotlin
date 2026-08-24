/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi;

import kotlin.SubclassOptInRequired;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Represents a declaration that may declare type parameters, such as a class, function, or type alias.
 *
 * <p>Type parameters are declared in angle brackets. The list's position depends on the declaration: for example, it follows a class name
 * but precedes a function name. Upper bounds may be written inline or in a separate {@code where} clause, which is exposed via
 * {@link #getTypeConstraintList()}.
 *
 * <h3>Example:</h3>
 * <pre>{@code
 * fun <T> copy(items: List<T>): List<T> where T : Cloneable = ...
 * //  ^_^                               ^_________________^
 * //  Type parameter list               Type constraint list ('where' clause)
 * }</pre>
 */
@SubclassOptInRequired(markerClass = KtImplementationDetail.class)
public interface KtTypeParameterListOwner extends KtNamedDeclaration {
    /** Returns the angle-bracketed type parameter list, or {@code null} if this declaration has no type parameters. */
    @Nullable
    KtTypeParameterList getTypeParameterList();

    /** Returns the {@code where} clause listing the type constraints, or {@code null} if there is none. */
    @Nullable
    KtTypeConstraintList getTypeConstraintList();

    /** Returns the type constraints from the {@code where} clause, or an empty list if there is none. */
    @NotNull
    List<KtTypeConstraint> getTypeConstraints();

    /** Returns the declared type parameters, or an empty list if there are none. */
    @NotNull
    List<KtTypeParameter> getTypeParameters();
}
