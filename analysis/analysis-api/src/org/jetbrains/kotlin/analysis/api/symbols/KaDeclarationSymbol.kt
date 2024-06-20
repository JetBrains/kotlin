/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.symbols

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaAnnotatedSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaTypeParameterOwnerSymbol

/**
 * Represents a symbol of declaration which can be directly expressed in source code.
 * Eg, classes, type parameters or functions are [KaDeclarationSymbol], but files and packages are not
 */
public sealed interface KaDeclarationSymbol : KaSymbol, KaAnnotatedSymbol

@KaExperimentalApi
public val KaDeclarationSymbol.typeParameters: List<KaTypeParameterSymbol>
    @OptIn(KaImplementationDetail::class)
    get() = if (this is KaTypeParameterOwnerSymbol) typeParameters else emptyList()


@Deprecated("Use 'KaDeclarationSymbol' instead", ReplaceWith("KaDeclarationSymbol"))
public typealias KtDeclarationSymbol = KaDeclarationSymbol