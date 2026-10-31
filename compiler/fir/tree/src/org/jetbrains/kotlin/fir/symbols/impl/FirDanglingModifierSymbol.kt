/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.symbols.impl

import org.jetbrains.kotlin.fir.declarations.FirDanglingModifierList
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.id.FirSymbolId
import org.jetbrains.kotlin.fir.symbols.id.FirUniqueSymbolId

class FirDanglingModifierSymbol(
    symbolId: FirSymbolId<FirDanglingModifierSymbol>,
) : FirBasedSymbol<FirDanglingModifierList>(symbolId) {
    /**
     * Creates a [FirDanglingModifierSymbol] with a [unique symbol ID][FirUniqueSymbolId].
     * See [FirBasedSymbol] for when to use this default constructor.
     */
    constructor() : this(FirUniqueSymbolId())

    override fun toString(): String = "${this::class.simpleName}"
}
