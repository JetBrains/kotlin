/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.validation.checkers.context

import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.validation.IrValidationError
import org.jetbrains.kotlin.ir.validation.ScopeStack
import org.jetbrains.kotlin.ir.validation.checkers.IrChecker

class CheckerContext(
    val irBuiltIns: IrBuiltIns,
    val file: IrFile,
    private val reportError: (IrValidationError) -> Unit,
) {
    val parentChain: MutableList<IrElement> = mutableListOf()
    val typeParameterScopeStack = ScopeStack<IrTypeParameterSymbol>()
    val valueSymbolScopeStack = ScopeStack<IrValueSymbol>()
    val offsetRanges: MutableList<OffsetRange> = mutableListOf()

    var withinAnnotationUsageSubTree: Boolean = false
        private set

    fun error(element: IrElement, cause: IrValidationError.Cause, message: String) =
        reportError(IrValidationError(file, element, cause, message, parentChain))

    context(checker: IrChecker)
    fun error(element: IrElement, message: String) = error(element, checker, message)

    fun withinAnnotationUsageSubTree(block: () -> Unit) {
        if (withinAnnotationUsageSubTree) {
            block()
        } else {
            withinAnnotationUsageSubTree = true
            block()
            withinAnnotationUsageSubTree = false
        }
    }
}