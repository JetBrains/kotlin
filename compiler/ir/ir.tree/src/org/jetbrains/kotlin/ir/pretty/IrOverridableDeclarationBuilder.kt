/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.pretty

import org.jetbrains.kotlin.ir.declarations.IrOverridableDeclaration
import org.jetbrains.kotlin.ir.symbols.IrSymbol

interface IrOverridableDeclarationBuilder: IrDeclarationWithModalityBuilder, IrDeclarationWithVisibilityBuilder, IrSymbolOwnerBuilder {

    var isFakeOverride: Boolean

    var overriddenSymbols: MutableList<String>

    class OverriddenSymbolListBuilder @PublishedApi internal constructor() {

        @PublishedApi
        internal val symbolReferences = mutableListOf<String>()

        @PrettyIrDsl
        fun symbol(symbolReference: String) {
            symbolReferences.add(symbolReference)
        }
    }
}

@IrNodePropertyDsl
fun IrOverridableDeclarationBuilder.fakeOverride(isFakeOverride: Boolean = true) {
    this.isFakeOverride = isFakeOverride
}

@PrettyIrDsl
inline fun IrOverridableDeclarationBuilder.overriddenSymbols(block: IrOverridableDeclarationBuilder.OverriddenSymbolListBuilder.() -> Unit) {
    overriddenSymbols = IrOverridableDeclarationBuilder.OverriddenSymbolListBuilder().apply(block).symbolReferences
}

internal inline fun <reified Symbol : IrSymbol> IrOverridableDeclarationBuilder.addOverriddenSymbolsTo(
    declaration: IrOverridableDeclaration<Symbol>,
    createSymbol: () -> Symbol
) {
    declaration.overriddenSymbols = overriddenSymbols.map { buildingContext.getOrCreateSymbol(it, createSymbol) }
}
