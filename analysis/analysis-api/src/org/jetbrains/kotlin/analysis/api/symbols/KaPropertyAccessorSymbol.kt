/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
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
    @Suppress("DEPRECATION") KaSymbolWithKind {

    final override val isExtension: Boolean get() = withValidityAssertion { false }

    final override val typeParameters: List<KaTypeParameterSymbol>
        get() = withValidityAssertion { emptyList() }

    final override val contextReceivers: List<KaContextReceiver> get() = withValidityAssertion { emptyList() }

    public abstract val isDefault: Boolean
    public abstract val isInline: Boolean
    public abstract val isOverride: Boolean
    public abstract val hasBody: Boolean

    final override val location: KaSymbolLocation get() = withValidityAssertion { KaSymbolLocation.PROPERTY }

    abstract override fun createPointer(): KaSymbolPointer<KaPropertyAccessorSymbol>
}

@Deprecated("Use 'KaPropertyAccessorSymbol' instead", ReplaceWith("KaPropertyAccessorSymbol"))
public typealias KtPropertyAccessorSymbol = KaPropertyAccessorSymbol

public abstract class KaPropertyGetterSymbol : KaPropertyAccessorSymbol() {
    abstract override fun createPointer(): KaSymbolPointer<KaPropertyGetterSymbol>
}

@Deprecated("Use 'KaPropertyGetterSymbol' instead", ReplaceWith("KaPropertyGetterSymbol"))
public typealias KtPropertyGetterSymbol = KaPropertyGetterSymbol

public abstract class KaPropertySetterSymbol : KaPropertyAccessorSymbol() {
    public abstract val parameter: KaValueParameterSymbol

    abstract override fun createPointer(): KaSymbolPointer<KaPropertySetterSymbol>
}

@Deprecated("Use 'KaPropertySetterSymbol' instead", ReplaceWith("KaPropertySetterSymbol"))
public typealias KtPropertySetterSymbol = KaPropertySetterSymbol