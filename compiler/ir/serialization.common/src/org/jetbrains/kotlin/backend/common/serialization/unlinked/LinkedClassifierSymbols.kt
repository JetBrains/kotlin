/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization.unlinked

import gnu.trove.THashMap
import gnu.trove.THashSet
import org.jetbrains.kotlin.backend.common.serialization.unlinked.LinkedClassifierStatus.Fully
import org.jetbrains.kotlin.backend.common.serialization.unlinked.LinkedClassifierStatus.Partially
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol

internal class LinkedClassifierSymbols {
    private val fullyLinkedSymbols = THashSet<IrClassifierSymbol>()
    private val partiallyLinkedSymbols = THashMap<IrClassifierSymbol, Partially>()

    operator fun get(symbol: IrClassifierSymbol): LinkedClassifierStatus? =
        if (symbol in fullyLinkedSymbols) Fully else partiallyLinkedSymbols[symbol]

    fun registerPartiallyLinked(symbol: IrClassifierSymbol, reason: Partially): Partially {
        partiallyLinkedSymbols[symbol] = reason
        return reason
    }

    fun registerFullyLinked(symbol: IrClassifierSymbol): Fully {
        fullyLinkedSymbols += symbol
        return Fully
    }
}
