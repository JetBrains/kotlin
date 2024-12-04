/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.linkage.issues

import org.jetbrains.kotlin.backend.common.serialization.KotlinIrLinker
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.messageCollector
import org.jetbrains.kotlin.ir.util.SymbolTable

// N.B. Checks for absence of unbound symbols only when unbound symbols are not allowed.
fun KotlinIrLinker.checkNoUnboundSymbols(symbolTable: SymbolTable, whenDetected: String): Unit =
    messageCollector.checkNoUnboundSymbols(symbolTable, whenDetected)

// N.B. Always checks for absence of unbound symbols. The condition whether this check should be applied is controlled outside.
fun MessageCollector.checkNoUnboundSymbols(symbolTable: SymbolTable, whenDetected: String) {
    val unboundSymbols = symbolTable.descriptorExtension.allUnboundSymbols
    if (unboundSymbols.isNotEmpty())
        UnexpectedUnboundIrSymbols(unboundSymbols, whenDetected).raiseIssue(this)
}

// N.B. Always checks for absence of unbound symbols. The condition whether this check should be applied is controlled outside.
fun CompilerConfiguration.checkNoUnboundSymbols(symbolTable: SymbolTable, whenDetected: String) {
    val unboundSymbols = symbolTable.descriptorExtension.allUnboundSymbols
    if (unboundSymbols.isNotEmpty())
        UnexpectedUnboundIrSymbols(unboundSymbols, whenDetected).raiseIssue(messageCollector)
}
