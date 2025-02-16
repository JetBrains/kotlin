/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.symbols

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility

/**
 * [KaClassInitializerSymbol] represents an [anonymous initializer declaration](https://kotlinlang.org/docs/reference/grammar.html#anonymousInitializer)
 * in a class body.
 */
public abstract class KaClassInitializerSymbol : KaDeclarationSymbol {
    final override val modality: KaSymbolModality get() = withValidityAssertion { KaSymbolModality.FINAL }
    final override val isActual: Boolean get() = withValidityAssertion { false }
    final override val isExpect: Boolean get() = withValidityAssertion { false }
    final override val location: KaSymbolLocation get() = withValidityAssertion { KaSymbolLocation.CLASS }

    @KaExperimentalApi
    final override val compilerVisibility: Visibility get() = withValidityAssertion { Visibilities.Local }

    abstract override fun createPointer(): KaSymbolPointer<KaClassInitializerSymbol>
}
