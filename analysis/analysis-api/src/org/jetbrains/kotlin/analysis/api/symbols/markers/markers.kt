/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.symbols.markers

import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaTypeParameterSymbol
import org.jetbrains.kotlin.name.Name

/**
 * @see org.jetbrains.kotlin.analysis.api.symbols.name
 */
@Deprecated("This API will be dropped soon. Use `KaSymbol.name`")
public interface KaPossiblyNamedSymbol : KaSymbol {
    public val name: Name?
}

@Deprecated("Use 'KaPossiblyNamedSymbol' instead", ReplaceWith("KaPossiblyNamedSymbol"))
public typealias KtPossiblyNamedSymbol = @Suppress("DEPRECATION") KaPossiblyNamedSymbol

public interface KaNamedSymbol : @Suppress("DEPRECATION") KaPossiblyNamedSymbol {
    override val name: Name
}

@Deprecated("Use 'KaNamedSymbol' instead", ReplaceWith("KaNamedSymbol"))
public typealias KtNamedSymbol = KaNamedSymbol

/**
 * Shouldn't be used as a type.
 * Consider using [typeParameters] directly from required class or [org.jetbrains.kotlin.analysis.api.symbols.typeParameters]
 *
 * @see org.jetbrains.kotlin.analysis.api.symbols.typeParameters
 */
@KaImplementationDetail
public interface KaTypeParameterOwnerSymbol : KaSymbol {
    public val typeParameters: List<KaTypeParameterSymbol>
}

@Deprecated("Use 'KaTypeParameterOwnerSymbol' instead", ReplaceWith("KaTypeParameterOwnerSymbol"))
@KaImplementationDetail
public typealias KaSymbolWithTypeParameters = KaTypeParameterOwnerSymbol

@Deprecated("Use 'KaTypeParameterOwnerSymbol' instead", ReplaceWith("KaTypeParameterOwnerSymbol"))
@KaImplementationDetail
public typealias KtSymbolWithTypeParameters = KaTypeParameterOwnerSymbol

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