/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.linkage.partial

import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.linkage.partial.ExploredClassifier

internal class ExploredClassifiers {
    private val usableSymbols = HashSet<IrClassifierSymbol>()
    private val unusableSymbols = HashMap<IrClassifierSymbol, ExploredClassifier.Unusable>()

    operator fun get(symbol: IrClassifierSymbol): ExploredClassifier? =
        if (symbol in usableSymbols) ExploredClassifier.Usable else unusableSymbols[symbol]

    fun registerUnusable(symbol: IrClassifierSymbol, exploredClassifier: ExploredClassifier.Unusable): ExploredClassifier.Unusable {
        unusableSymbols[symbol] = exploredClassifier
        return exploredClassifier
    }

    fun registerUsable(symbol: IrClassifierSymbol): ExploredClassifier.Usable {
        usableSymbols += symbol
        return ExploredClassifier.Usable
    }
}
