/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.symbols

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.base.KaContextReceiver
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer

public sealed class KaPropertyAccessorSymbol : KaFunctionSymbol(){

    override val isExtension: Boolean get() = withValidityAssertion { false }

    override val isExpect: Boolean get() = withValidityAssertion { false }

    /**
     * Property accessors cannot have `actual` modifier in valid code
     * as this modifier is not propagated from containing declarations as it done
     * for `expect` modifier.
     */
    final override val isActual: Boolean get() = withValidityAssertion { false }

    @KaExperimentalApi
    final override val contextReceivers: List<KaContextReceiver> get() = withValidityAssertion { emptyList() }

    public abstract val isDefault: Boolean
    public abstract val isInline: Boolean
    public abstract val isOverride: Boolean
    public abstract val hasBody: Boolean

    final override val location: KaSymbolLocation get() = withValidityAssertion { KaSymbolLocation.PROPERTY }

    abstract override fun createPointer(): KaSymbolPointer<KaPropertyAccessorSymbol>
}

public abstract class KaPropertyGetterSymbol : KaPropertyAccessorSymbol() {
    final override val valueParameters: List<KaValueParameterSymbol>
        get() = withValidityAssertion { emptyList() }

    final override val hasStableParameterNames: Boolean
        get() = withValidityAssertion { true }

    abstract override fun createPointer(): KaSymbolPointer<KaPropertyGetterSymbol>
}

public abstract class KaPropertySetterSymbol : KaPropertyAccessorSymbol() {
    public abstract val parameter: KaValueParameterSymbol

    final override val valueParameters: List<KaValueParameterSymbol>
        get() = withValidityAssertion { listOf(parameter) }

    abstract override fun createPointer(): KaSymbolPointer<KaPropertySetterSymbol>
}
