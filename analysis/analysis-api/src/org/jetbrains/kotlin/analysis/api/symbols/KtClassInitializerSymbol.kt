/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.symbols

import org.jetbrains.kotlin.analysis.api.symbols.markers.KaSymbolWithKind

public abstract class KaClassInitializerSymbol : KaDeclarationSymbol, KaSymbolWithKind

public typealias KtClassInitializerSymbol = KaClassInitializerSymbol