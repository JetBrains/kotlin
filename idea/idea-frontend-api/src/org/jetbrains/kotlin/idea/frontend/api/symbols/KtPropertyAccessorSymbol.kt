/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.symbols

import org.jetbrains.kotlin.idea.frontend.api.symbols.markers.KtCommonSymbolModality
import org.jetbrains.kotlin.idea.frontend.api.symbols.markers.KtSymbolWithModality
import org.jetbrains.kotlin.idea.frontend.api.symbols.markers.KtSymbolWithVisibility
import org.jetbrains.kotlin.idea.frontend.api.symbols.markers.KtTypedSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.pointers.KtSymbolPointer

sealed class KtPropertyAccessorSymbol : KtFunctionLikeSymbol(),
    KtSymbolWithModality<KtCommonSymbolModality>,
    KtSymbolWithVisibility {
    abstract val isDefault: Boolean

    abstract override fun createPointer(): KtSymbolPointer<KtPropertyAccessorSymbol>
}

abstract class KtPropertyGetterSymbol : KtPropertyAccessorSymbol(), KtTypedSymbol {
    abstract override fun createPointer(): KtSymbolPointer<KtPropertyGetterSymbol>
}

abstract class KtPropertySetterSymbol : KtPropertyAccessorSymbol() {
    abstract val parameter: KtSetterParameterSymbol

    abstract override fun createPointer(): KtSymbolPointer<KtPropertySetterSymbol>
}