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

    val buildingContext: IrBuildingContext
}

@PrettyIrDsl
fun IrSymbolOwnerBuilder.symbol(symbolReference: String) {
    this.symbolReference = symbolReference
}

// TODO: We need a way to work with public/private symbols. Right now there are only private symbols.
internal inline fun <reified Symbol : IrSymbol> IrSymbolOwnerBuilder.symbol(symbolConstructor: () -> Symbol): Symbol =
    buildingContext.getOrCreateSymbol(symbolReference, symbolConstructor)

internal fun IrSymbolOwnerBuilder.recordSymbolFromOwner(owner: IrSymbolOwner) =
    buildingContext.putSymbolForSymbolOwner(owner)


