/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi;

import com.intellij.util.ArrayFactory;
import kotlin.SubclassOptInRequired;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Represents the actual type syntax inside a {@link KtTypeReference}, stripped of any leading annotations and modifiers.
 *
 * <p>This is the common base type for the concrete type-syntax nodes, such as {@link KtUserType} ({@code Foo<Bar>}), {@link KtNullableType}
 * ({@code Foo?}), {@link KtFunctionType} ({@code (Int) -> String}), and {@link KtDynamicType} ({@code dynamic}). A {@link KtTypeReference}
 * wraps exactly one {@link KtTypeElement}.
 *
 * @see KtTypeReference
 */
@SubclassOptInRequired(markerClass = KtImplementationDetail.class)
public interface KtTypeElement extends KtElement {
    /** A shared empty array, which can be reused to avoid unnecessary allocations. */
    KtTypeElement[] EMPTY_ARRAY = new KtTypeElement[0];

    /** A factory for creating arrays of {@link KtTypeElement}, used by the PSI child-access machinery. */
    ArrayFactory<KtTypeElement> ARRAY_FACTORY = count -> count == 0 ? EMPTY_ARRAY : new KtTypeElement[count];

    /**
     * Returns the type arguments of this type element as type references (for example, {@code Int} and {@code String} for
     * {@code Map<Int, String>}), or an empty list if there are none.
     *
     * <p>The list may contain {@code null} elements for malformed type arguments in incomplete code.
     */
    @NotNull
    List<KtTypeReference> getTypeArgumentsAsTypes();
}
