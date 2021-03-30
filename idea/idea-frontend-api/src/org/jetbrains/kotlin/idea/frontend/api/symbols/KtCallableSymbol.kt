/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.symbols

import org.jetbrains.kotlin.idea.frontend.api.symbols.markers.KtTypedSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.name.CallableId

abstract class KtCallableSymbol : KtSymbol, KtTypedSymbol {
    abstract val callableIdIfNonLocal: CallableId?
    abstract override fun createPointer(): KtSymbolPointer<KtCallableSymbol>
}