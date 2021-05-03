/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.symbols

import org.jetbrains.kotlin.idea.frontend.api.symbols.markers.*
import org.jetbrains.kotlin.idea.frontend.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.name.CallableId

sealed class KtPropertyAccessorSymbol : KtCallableSymbol(),
    KtPossibleMemberSymbol,
    KtSymbolWithModality,
    KtSymbolWithVisibility,
    KtAnnotatedSymbol,
    KtSymbolWithKind {

    final override val callableIdIfNonLocal: CallableId? get() = null
    final override val isExtension: Boolean get() = false
    final override val receiverType: KtTypeAndAnnotations? get() = null

    abstract val isDefault: Boolean
    abstract val isInline: Boolean
    abstract val isOverride: Boolean
    abstract val hasBody: Boolean

    final override val symbolKind: KtSymbolKind get() = KtSymbolKind.ACCESSOR

    abstract override fun createPointer(): KtSymbolPointer<KtPropertyAccessorSymbol>
}

abstract class KtPropertyGetterSymbol : KtPropertyAccessorSymbol() {
    abstract override fun createPointer(): KtSymbolPointer<KtPropertyGetterSymbol>
}

abstract class KtPropertySetterSymbol : KtPropertyAccessorSymbol() {
    abstract val parameter: KtValueParameterSymbol

    abstract override fun createPointer(): KtSymbolPointer<KtPropertySetterSymbol>
}