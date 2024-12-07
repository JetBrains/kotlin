/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.symbols

import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaAnnotatedSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer

/**
 * [KaFileSymbol] represents a [KtFile][org.jetbrains.kotlin.psi.KtFile].
 *
 * The symbol may for example be used to retrieve and analyze the file's [annotations].
 */
public abstract class KaFileSymbol : KaAnnotatedSymbol {
    final override val location: KaSymbolLocation
        get() = withValidityAssertion { KaSymbolLocation.TOP_LEVEL }

    abstract override fun createPointer(): KaSymbolPointer<KaFileSymbol>
}
