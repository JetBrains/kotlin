/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.symbols

import org.jetbrains.kotlin.analysis.api.base.KaContextReceiver
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.markers.*
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer

public sealed class KaPropertyAccessorSymbol : KaFunctionLikeSymbol(),
    KaPossibleMemberSymbol,
    KaSymbolWithModality,
    KaSymbolWithVisibility,
    KaSymbolWithKind {

    final override val isExtension: Boolean get() = withValidityAssertion { false }

    final override val typeParameters: List<KaTypeParameterSymbol>
        get() = withValidityAssertion { emptyList() }

    final override val contextReceivers: List<KaContextReceiver> get() = withValidityAssertion { emptyList() }

    public abstract val isDefault: Boolean
    public abstract val isInline: Boolean
    public abstract val isOverride: Boolean
    public abstract val hasBody: Boolean

    final override val symbolKind: KaSymbolKind get() = withValidityAssertion { KaSymbolKind.ACCESSOR }

    abstract override fun createPointer(): KaSymbolPointer<KaPropertyAccessorSymbol>
}

public typealias KtPropertyAccessorSymbol = KaPropertyAccessorSymbol

public abstract class KaPropertyGetterSymbol : KaPropertyAccessorSymbol() {
    abstract override fun createPointer(): KaSymbolPointer<KaPropertyGetterSymbol>
}

public typealias KtPropertyGetterSymbol = KaPropertyGetterSymbol

public abstract class KaPropertySetterSymbol : KaPropertyAccessorSymbol() {
    public abstract val parameter: KaValueParameterSymbol

    abstract override fun createPointer(): KaSymbolPointer<KaPropertySetterSymbol>
}

public typealias KtPropertySetterSymbol = KaPropertySetterSymbol