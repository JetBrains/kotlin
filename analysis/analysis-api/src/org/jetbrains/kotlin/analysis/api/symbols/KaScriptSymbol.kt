/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.symbols

import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaAnnotatedSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaNamedSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaSymbolWithMembers
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer

public abstract class KaScriptSymbol : KaDeclarationSymbol, KaAnnotatedSymbol, KaNamedSymbol, KaSymbolWithMembers {
    abstract override fun createPointer(): KaSymbolPointer<KaScriptSymbol>

    final override val location: KaSymbolLocation
        get() = withValidityAssertion { KaSymbolLocation.TOP_LEVEL }
}

@Deprecated("Use 'KaScriptSymbol' instead", ReplaceWith("KaScriptSymbol"))
public typealias KtScriptSymbol = KaScriptSymbol