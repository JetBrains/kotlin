/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.symbols

import org.jetbrains.kotlin.analysis.api.base.KtContextReceiver
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.markers.*
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer

public sealed class KtPropertyAccessorSymbol : KtFunctionLikeSymbol(),
    KtPossibleMemberSymbol,
    KtSymbolWithModality,
    KtSymbolWithVisibility,
    KtSymbolWithKind {

    final override val isExtension: Boolean get() = withValidityAssertion { false }

    final override val typeParameters: List<KtTypeParameterSymbol>
        get() = withValidityAssertion { emptyList() }

    final override val contextReceivers: List<KtContextReceiver> get() = withValidityAssertion { emptyList() }

    public abstract val isDefault: Boolean
    public abstract val isInline: Boolean
    public abstract val isOverride: Boolean
    public abstract val hasBody: Boolean

    final override val symbolKind: KtSymbolKind get() = withValidityAssertion { KtSymbolKind.ACCESSOR }

    abstract override fun createPointer(): KtSymbolPointer<KtPropertyAccessorSymbol>
}

public abstract class KtPropertyGetterSymbol : KtPropertyAccessorSymbol() {
    abstract override fun createPointer(): KtSymbolPointer<KtPropertyGetterSymbol>
}

public abstract class KtPropertySetterSymbol : KtPropertyAccessorSymbol() {
    public abstract val parameter: KtValueParameterSymbol

    abstract override fun createPointer(): KtSymbolPointer<KtPropertySetterSymbol>
}
