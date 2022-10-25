/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization.linkerissues

import org.jetbrains.kotlin.backend.common.serialization.KotlinIrLinker
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.ir.declarations.IrAnnotationContainer
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.IrMessageLogger
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.ir.util.allUnbound
import org.jetbrains.kotlin.ir.util.irMessageLogger
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

@OptIn(ExperimentalContracts::class)
internal inline fun <reified T : IrSymbol> checkSymbolType(symbol: IrSymbol): T {
    contract {
        returns() implies (symbol is T)
    }

    if (symbol !is T) throw IrSymbolTypeMismatchException(T::class.java, symbol) else return symbol
}

@OptIn(ExperimentalContracts::class)
internal inline fun <reified T : IrAnnotationContainer> checkErrorNodesAllowed(errorNodesAllowed: Boolean) {
    contract {
        returns() implies errorNodesAllowed
    }
    if (!errorNodesAllowed) throw IrDisallowedErrorNode(T::class.java)
}

// N.B. Checks for absence of unbound symbols only when unbound symbols are not allowed.
fun KotlinIrLinker.checkNoUnboundSymbols(symbolTable: SymbolTable, whenDetected: String): Unit =
    messageLogger.checkNoUnboundSymbols(symbolTable, whenDetected)

// N.B. Always checks for absence of unbound symbols. The condition whether this check should be applied is controlled outside.
fun IrMessageLogger.checkNoUnboundSymbols(symbolTable: SymbolTable, whenDetected: String) {
    val unboundSymbols = symbolTable.allUnbound
    if (unboundSymbols.isNotEmpty())
        UnexpectedUnboundIrSymbols(unboundSymbols, whenDetected).raiseIssue(this)
}

// N.B. Always checks for absence of unbound symbols. The condition whether this check should be applied is controlled outside.
fun CompilerConfiguration.checkNoUnboundSymbols(symbolTable: SymbolTable, whenDetected: String) {
    val unboundSymbols = symbolTable.allUnbound
    if (unboundSymbols.isNotEmpty())
        UnexpectedUnboundIrSymbols(unboundSymbols, whenDetected).raiseIssue(irMessageLogger)
}
