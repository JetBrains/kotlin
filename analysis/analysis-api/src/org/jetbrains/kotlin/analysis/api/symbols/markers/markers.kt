/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.symbols.markers

import org.jetbrains.kotlin.analysis.api.symbols.KtSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtTypeParameterSymbol
import org.jetbrains.kotlin.name.Name

public interface KtPossiblyNamedSymbol : KtSymbol {
    public val name: Name?
}

public interface KtNamedSymbol : KtPossiblyNamedSymbol {
    override val name: Name
}

public interface KtSymbolWithTypeParameters : KtSymbol {
    public val typeParameters: List<KtTypeParameterSymbol>
}

/**
 * A marker interface for symbols which could potentially be `expect` or `actual`. For more details about `expect` and `actual`
 * declarations, see [documentation](https://kotlinlang.org/docs/multiplatform-connect-to-apis.html).
 */
public interface KtPossibleMultiplatformSymbol : KtSymbol {
    /**
     * Returns true if the declaration is a platform-specific implementation in a multiplatform project.
     */
    public val isActual: Boolean

    /**
     * Returns true if the declaration is platform-specific declaration in a multiplatform project. An implementation
     * in platform modules is expected. Note, that in the following example:
     * ```
     * expect class A {
     *     class Nested
     * }
     * ```
     * `isExpect` returns `true` for both `A` and `A.Nested`.
     */
    public val isExpect: Boolean
}