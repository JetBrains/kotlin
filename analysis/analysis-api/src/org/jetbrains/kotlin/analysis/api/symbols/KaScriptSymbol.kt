/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.symbols

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaAnnotatedSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaDeclarationContainerSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaNamedSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility

public abstract class KaScriptSymbol : KaDeclarationSymbol, KaAnnotatedSymbol, KaNamedSymbol, KaDeclarationContainerSymbol {
    abstract override fun createPointer(): KaSymbolPointer<KaScriptSymbol>

    final override val location: KaSymbolLocation get() = withValidityAssertion { KaSymbolLocation.TOP_LEVEL }
    final override val modality: KaSymbolModality get() = withValidityAssertion { KaSymbolModality.FINAL }
    final override val isActual: Boolean get() = withValidityAssertion { false }
    final override val isExpect: Boolean get() = withValidityAssertion { false }

    @KaExperimentalApi
    final override val compilerVisibility: Visibility get() = withValidityAssertion { Visibilities.Local }
}

@Deprecated("Use 'KaScriptSymbol' instead", ReplaceWith("KaScriptSymbol"))
public typealias KtScriptSymbol = KaScriptSymbol