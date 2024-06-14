/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.symbols


public abstract class KaClassInitializerSymbol : KaDeclarationSymbol,
    @Suppress("DEPRECATION") org.jetbrains.kotlin.analysis.api.symbols.markers.KaSymbolWithKind

@Deprecated("Use 'KaClassInitializerSymbol'", ReplaceWith("KaClassInitializerSymbol"))
public typealias KtClassInitializerSymbol = KaClassInitializerSymbol