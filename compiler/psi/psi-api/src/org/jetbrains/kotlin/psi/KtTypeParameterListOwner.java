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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Represents a declaration that may declare type parameters, such as a class, function, or type alias.
 *
 * <p>Type parameters are declared in angle brackets right after the declaration keyword and name. Upper bounds may be
 * written inline or in a separate {@code where} clause, which is exposed via {@link #getTypeConstraintList()}.
 *
 * <h3>Example:</h3>
 * <pre>{@code
 * fun <T> copy(items: List<T>): List<T> where T : Cloneable = ...
 * //  ^_^                               ^_________________^
 * //  Type parameter list               Type constraint list ('where' clause)
 * }</pre>
 */
public interface KtTypeParameterListOwner extends KtNamedDeclaration {
    /**
     * Returns the angle-bracketed type parameter list, or {@code null} if this declaration has no type parameters.
     */
    @Nullable
    KtTypeParameterList getTypeParameterList();

    /**
     * Returns the {@code where} clause listing the type constraints, or {@code null} if there is none.
     */
    @Nullable
    KtTypeConstraintList getTypeConstraintList();

    /**
     * Returns the type constraints from the {@code where} clause, or an empty list if there is none.
     */
    @NotNull
    List<KtTypeConstraint> getTypeConstraints();

    /**
     * Returns the declared type parameters, or an empty list if there are none.
     */
    @NotNull
    List<KtTypeParameter> getTypeParameters();
}
