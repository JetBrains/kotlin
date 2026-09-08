/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi;

import kotlin.SubclassOptInRequired;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a declaration that can have an expression after {@code =}, such as a property, a destructuring declaration, a named function,
 * or a property accessor.
 *
 * <h3>Example:</h3>
 * <pre>{@code
 * val greeting = "Hello"
 * //             ^_____^
 * // The initializer
 * }</pre>
 */
@SubclassOptInRequired(markerClass = KtImplementationDetail.class)
public interface KtDeclarationWithInitializer extends KtDeclaration {
    /** Returns the initializer expression, or {@code null} if this declaration has none. */
    @Nullable
    KtExpression getInitializer();

    /** Returns {@code true} if this declaration has an initializer expression. */
    boolean hasInitializer();
}
