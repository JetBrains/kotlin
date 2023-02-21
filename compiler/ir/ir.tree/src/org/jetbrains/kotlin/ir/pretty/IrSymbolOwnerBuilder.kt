/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.pretty

import org.jetbrains.kotlin.ir.declarations.IrSymbolOwner
import org.jetbrains.kotlin.ir.symbols.IrSymbol

@PrettyIrDsl
interface IrSymbolOwnerBuilder {

    var symbolReference: String?

    val symbolContext: SymbolContext

    @PrettyIrDsl
    fun symbol(symbolReference: String) {
        this.symbolReference = symbolReference
    }
}

internal inline fun <reified Symbol : IrSymbol> IrSymbolOwnerBuilder.symbol(noinline symbolConstructor: () -> Symbol): Symbol =
    symbolContext.symbol(symbolReference, symbolConstructor)

internal fun IrSymbolOwnerBuilder.recordSymbolFromOwner(owner: IrSymbolOwner) =
    symbolContext.putSymbolForOwner(owner)


