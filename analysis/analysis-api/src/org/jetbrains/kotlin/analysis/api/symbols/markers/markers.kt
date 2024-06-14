/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.symbols.markers

import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaTypeParameterSymbol
import org.jetbrains.kotlin.name.Name

public interface KaPossiblyNamedSymbol : KaSymbol {
    public val name: Name?
}

@Deprecated("Use 'KaPossiblyNamedSymbol' instead", ReplaceWith("KaPossiblyNamedSymbol"))
public typealias KtPossiblyNamedSymbol = KaPossiblyNamedSymbol

public interface KaNamedSymbol : KaPossiblyNamedSymbol {
    override val name: Name
}

@Deprecated("Use 'KaNamedSymbol' instead", ReplaceWith("KaNamedSymbol"))
public typealias KtNamedSymbol = KaNamedSymbol

public interface KaSymbolWithTypeParameters : KaSymbol {
    public val typeParameters: List<KaTypeParameterSymbol>
}

@Deprecated("Use 'KaSymbolWithTypeParameters' instead", ReplaceWith("KaSymbolWithTypeParameters"))
public typealias KtSymbolWithTypeParameters = KaSymbolWithTypeParameters

/**
 * A marker interface for symbols which could potentially be `expect` or `actual`. For more details about `expect` and `actual`
 * declarations, see [documentation](https://kotlinlang.org/docs/multiplatform-connect-to-apis.html).
 */
public interface KaPossibleMultiplatformSymbol : KaSymbol {
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

@Deprecated("Use 'KaPossibleMultiplatformSymbol' instead", ReplaceWith("KaPossibleMultiplatformSymbol"))
public typealias KtPossibleMultiplatformSymbol = KaPossibleMultiplatformSymbol