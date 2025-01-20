/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.symbols

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaDeclarationContainerSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaNamedSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility

/**
 * [KaScriptSymbol] represents a [Kotlin script](https://kotlinlang.org/docs/custom-script-deps-tutorial.html).
 *
 * Conceptually, a script is an intermediate layer between the outer [KtFile][org.jetbrains.kotlin.psi.KtFile] and the top-level
 * declarations contained in it. The [KaScriptSymbol] is the [containingDeclaration][org.jetbrains.kotlin.analysis.api.components.KaSymbolRelationProvider.containingDeclaration]
 * of its contained top-level declarations.
 */
public abstract class KaScriptSymbol : KaDeclarationSymbol, KaNamedSymbol, KaDeclarationContainerSymbol {
    final override val location: KaSymbolLocation get() = withValidityAssertion { KaSymbolLocation.TOP_LEVEL }
    final override val modality: KaSymbolModality get() = withValidityAssertion { KaSymbolModality.FINAL }
    final override val isActual: Boolean get() = withValidityAssertion { false }
    final override val isExpect: Boolean get() = withValidityAssertion { false }

    @KaExperimentalApi
    final override val compilerVisibility: Visibility get() = withValidityAssertion { Visibilities.Local }

    abstract override fun createPointer(): KaSymbolPointer<KaScriptSymbol>
}
