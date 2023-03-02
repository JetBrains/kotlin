/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.pretty

import org.jetbrains.kotlin.ir.declarations.IrFactory
import org.jetbrains.kotlin.ir.declarations.IrSymbolOwner
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.symbols.*
import kotlin.reflect.KClass

internal inline fun <reified Symbol : IrSymbol> IrBuildingContext.symbol(name: String?, noinline createSymbol: () -> Symbol): Symbol =
    symbol(name, Symbol::class, createSymbol)

class IrBuildingContext(val irFactory: IrFactory = IrFactoryImpl) {

    private class SymbolMap<Symbol : IrSymbol> {
        private val string2SymbolMap = mutableMapOf<String, Symbol>()
        private val symbol2StringMap = mutableMapOf<Symbol, String>()

        fun getOrCreateSymbol(
            name: String?,
            symbolConstructor: () -> Symbol,
        ): Symbol {
            if (name == null) return symbolConstructor()
            string2SymbolMap[name]?.let { return it }
            return symbolConstructor().also { putSymbol(name, it) }
        }

        fun putSymbol(name: String, symbol: IrSymbol) {
            @Suppress("UNCHECKED_CAST")
            string2SymbolMap[name] = symbol as Symbol
            symbol2StringMap[symbol] = name
        }

        fun containsSymbol(symbol: IrSymbol) = symbol2StringMap.containsKey(symbol)
    }

    private inline fun <reified Symbol : IrSymbol> symbolClass2SymbolMap() = Symbol::class to SymbolMap<Symbol>()

    private val symbolMaps: Map<KClass<out IrSymbol>, SymbolMap<out IrSymbol>> = mapOf(
        symbolClass2SymbolMap<IrFileSymbol>(),
        symbolClass2SymbolMap<IrExternalPackageFragmentSymbol>(),
        symbolClass2SymbolMap<IrAnonymousInitializerSymbol>(),
        symbolClass2SymbolMap<IrEnumEntrySymbol>(),
        symbolClass2SymbolMap<IrFieldSymbol>(),
        symbolClass2SymbolMap<IrClassSymbol>(),
        symbolClass2SymbolMap<IrScriptSymbol>(),
        symbolClass2SymbolMap<IrTypeParameterSymbol>(),
        symbolClass2SymbolMap<IrValueParameterSymbol>(),
        symbolClass2SymbolMap<IrVariableSymbol>(),
        symbolClass2SymbolMap<IrConstructorSymbol>(),
        symbolClass2SymbolMap<IrSimpleFunctionSymbol>(),
        symbolClass2SymbolMap<IrReturnableBlockSymbol>(),
        symbolClass2SymbolMap<IrPropertySymbol>(),
        symbolClass2SymbolMap<IrLocalDelegatedPropertySymbol>(),
        symbolClass2SymbolMap<IrTypeAliasSymbol>(),
    )

    fun <Symbol : IrSymbol> symbol(name: String?, symbolClass: KClass<Symbol>, createSymbol: () -> Symbol): Symbol {
        @Suppress("UNCHECKED_CAST")
        val map = symbolMaps[symbolClass] as SymbolMap<Symbol>? ?: error("Symbol map not found for ${symbolClass.simpleName}")
        return map.getOrCreateSymbol(name, createSymbol)
    }

    private fun uniqueNameForSymbolOwner(owner: IrSymbolOwner): String {
        TODO("Not implemented yet")
    }

    private fun <Owner : IrSymbolOwner, Symbol : IrBindableSymbol<*, Owner>> putSymbolForSymbolOwner(owner: Owner, map: SymbolMap<Symbol>) {
        @Suppress("UNCHECKED_CAST")
        val symbol = owner.symbol as Symbol
        if (map.containsSymbol(symbol)) return
        val stringRepresentation = uniqueNameForSymbolOwner(owner)
        map.putSymbol(stringRepresentation, symbol)
    }

    fun putSymbolForOwner(owner: IrSymbolOwner) {
        val symbol = owner.symbol
        val map = symbolMaps[symbol::class] ?: error("Symbol map not found for ${symbol::class.simpleName}")
        if (map.containsSymbol(symbol)) return
        map.putSymbol(uniqueNameForSymbolOwner(owner), symbol)
    }
}
