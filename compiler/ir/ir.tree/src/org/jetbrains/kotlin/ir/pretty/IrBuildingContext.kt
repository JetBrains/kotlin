/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.pretty

import org.jetbrains.kotlin.ir.declarations.IrFactory
import org.jetbrains.kotlin.ir.declarations.IrSymbolOwner
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.symbols.*

class IrBuildingContext(val irFactory: IrFactory = IrFactoryImpl) {

    internal class SymbolTable {
        private val string2SymbolMap = mutableMapOf<String, IrSymbol>()
        private val symbol2StringMap = mutableMapOf<IrSymbol, String>()

        inline fun getOrCreateSymbol(
            name: String?,
            symbolConstructor: () -> IrSymbol,
        ): IrSymbol {
            if (name == null) return symbolConstructor()
            string2SymbolMap[name]?.let { return it }
            return symbolConstructor().also { putSymbol(name, it) }
        }

        fun putSymbol(name: String, symbol: IrSymbol) {
            string2SymbolMap[name] = symbol
            symbol2StringMap[symbol] = name
        }

        fun getSymbol(name: String) = string2SymbolMap[name]

        fun containsSymbol(symbol: IrSymbol) = symbol2StringMap.containsKey(symbol)
    }

    internal val symbolTable = SymbolTable()

    /**
     * If a symbol with a non-null [name] and with type [Symbol] is found in this building context, returns that symbol.
     *
     * If [name] is not `null` and no symbol with this name is found in this building context, creates a new symbol with [createSymbol],
     * saves that symbol in the context under the provided name and returns that symbol.
     *
     * If [name] is `null`, just executes [createSymbol] and returns it result without saving the created symbol in the building context.
     *
     * If a symbol with [name] is found in this context, but that symbol has a type different from [Symbol],
     * throws [IllegalArgumentException].
     *
     * @param name The name to associate with the symbol. Different symbols within a building context must have different names.
     * @param createSymbol A symbol constructor to execute if no symbol with [name] is found in this building context.
     * @return A symbol associated with [name]
     */
    internal inline fun <reified Symbol : IrSymbol> getOrCreateSymbol(name: String?, createSymbol: () -> Symbol): Symbol =
        when (val irSymbol = symbolTable.getOrCreateSymbol(name, createSymbol)) {
            is Symbol -> irSymbol
            else -> throw IllegalArgumentException(
                "Symbol associated with name '$name' is expected to have the type ${Symbol::class.simpleName}, " +
                        "but the actual type is ${irSymbol::class.simpleName}"
            )
        }

    internal inline fun <reified Symbol : IrSymbol> getSymbol(name: String): Symbol {
        return when (val irSymbol = symbolTable.getSymbol(name)) {
            null -> error("Symbol associated with name '$name' is not found")
            is Symbol -> irSymbol
            else -> throw IllegalArgumentException(
                "Symbol associated with name '$name' is expected to have the type ${Symbol::class.simpleName}, " +
                        "but the actual type is ${irSymbol::class.simpleName}"
            )
        }
    }

    private fun uniqueNameForSymbolOwner(owner: IrSymbolOwner): String {
        TODO("Not implemented yet")
    }

    fun putSymbolForSymbolOwner(owner: IrSymbolOwner) {
        val symbol = owner.symbol
        if (symbolTable.containsSymbol(symbol)) return
        val stringRepresentation = uniqueNameForSymbolOwner(owner)
        symbolTable.putSymbol(stringRepresentation, symbol)
    }
}
