/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.linkage.issues

import org.jetbrains.kotlin.backend.common.linkage.partial.PartialLinkageDiagnostics
import org.jetbrains.kotlin.backend.common.serialization.KotlinIrLinker
import org.jetbrains.kotlin.cli.report
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.ir.util.SymbolTable

// N.B. Checks for absence of unbound symbols only when unbound symbols are not allowed.
fun KotlinIrLinker.checkNoUnboundSymbols(symbolTable: SymbolTable, whenDetected: String) {
    checkNoUnboundSymbols(symbolTable, whenDetected, errorCallback)
}

fun CompilerConfiguration.checkNoUnboundSymbols(symbolTable: SymbolTable, whenDetected: String) {
    checkNoUnboundSymbols(symbolTable, whenDetected) { report(PartialLinkageDiagnostics.IR_LINKER_ERROR, it) }
}

// N.B. Always checks for absence of unbound symbols. The condition whether this check should be applied is controlled outside.
private fun checkNoUnboundSymbols(
    symbolTable: SymbolTable,
    whenDetected: String,
    consumer: (String) -> Unit,
) {
    val unboundSymbols = symbolTable.descriptorExtension.allUnboundSymbols
    if (unboundSymbols.isNotEmpty()) {
        UnexpectedUnboundIrSymbols(unboundSymbols, whenDetected).raiseIssue(consumer)
    }
}
