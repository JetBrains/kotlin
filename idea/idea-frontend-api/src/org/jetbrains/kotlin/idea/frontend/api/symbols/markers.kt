/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.symbols

import org.jetbrains.kotlin.idea.frontend.api.types.KtType
import org.jetbrains.kotlin.name.Name

interface KtNamedSymbol : KtSymbol {
    val name: Name
}

interface KtTypedSymbol : KtSymbol {
    val type: KtType
}

interface KtPossibleExtensionSymbol {
    val isExtension: Boolean
    val receiverType: KtType?
}

val KtCallableSymbol.isExtension: Boolean
    get() = (this as? KtPossibleExtensionSymbol)?.isExtension == true

interface KtSymbolWithTypeParameters {
    val typeParameters: List<KtTypeParameterSymbol>
}

interface KtSymbolWithModality<M : KtSymbolModality> {
    val modality: M
}

sealed class KtSymbolModality {
    object SEALED : KtSymbolModality()
}

sealed class KtCommonSymbolModality : KtSymbolModality() {
    object FINAL : KtCommonSymbolModality()
    object ABSTRACT : KtCommonSymbolModality()
    object OPEN : KtCommonSymbolModality()
}
