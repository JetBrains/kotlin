/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.symbols.markers

import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol
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

@Deprecated("Use 'KaDeclarationSymbol' directly", ReplaceWith("KaDeclarationSymbol"))
public typealias KaPossibleMultiplatformSymbol = KaDeclarationSymbol

@Deprecated("Use 'KaDeclarationSymbol' directly", ReplaceWith("KaDeclarationSymbol"))
public typealias KtPossibleMultiplatformSymbol = KaDeclarationSymbol