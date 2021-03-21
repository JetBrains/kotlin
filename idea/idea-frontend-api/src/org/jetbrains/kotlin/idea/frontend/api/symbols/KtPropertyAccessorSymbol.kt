/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.symbols

import org.jetbrains.kotlin.idea.frontend.api.symbols.markers.*
import org.jetbrains.kotlin.idea.frontend.api.symbols.pointers.KtSymbolPointer

sealed class KtPropertyAccessorSymbol : KtCallableSymbol(),
    KtPossibleMemberSymbol,
    KtSymbolWithModality<KtCommonSymbolModality>,
    KtSymbolWithVisibility,
    KtAnnotatedSymbol {

    abstract val isDefault: Boolean
    abstract val isInline: Boolean
    abstract val isOverride: Boolean
    abstract val hasBody: Boolean

    abstract val symbolKind: KtSymbolKind

    abstract override fun createPointer(): KtSymbolPointer<KtPropertyAccessorSymbol>
}

abstract class KtPropertyGetterSymbol : KtPropertyAccessorSymbol(), KtTypedSymbol {
    abstract override fun createPointer(): KtSymbolPointer<KtPropertyGetterSymbol>
}

abstract class KtPropertySetterSymbol : KtPropertyAccessorSymbol() {
    abstract val parameter: KtValueParameterSymbol

    abstract override fun createPointer(): KtSymbolPointer<KtPropertySetterSymbol>
}