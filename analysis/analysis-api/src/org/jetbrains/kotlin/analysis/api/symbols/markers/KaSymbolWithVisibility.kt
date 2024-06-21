/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.symbols.markers

import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility

@Deprecated("Use `KaDeclarationSymbol` directly", ReplaceWith("KaDeclarationSymbol"))
public typealias KaSymbolWithVisibility = KaDeclarationSymbol

@Deprecated("Use 'KaDeclarationSymbol' directly", ReplaceWith("KaDeclarationSymbol"))
public typealias KtSymbolWithVisibility = KaDeclarationSymbol

public fun Visibility.isPrivateOrPrivateToThis(): Boolean =
    this == Visibilities.Private || this == Visibilities.PrivateToThis
