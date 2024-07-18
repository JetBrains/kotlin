/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.util.isAccessor
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid

internal class NoInlineFunctionUseSitesValidator(
    private val file: IrFile,
    private val reportError: ReportIrValidationError,
    private val inlineFunctionUseSiteChecker: InlineFunctionUseSiteChecker,
) : IrElementVisitorVoid {
    private val parentChain = mutableListOf<IrElement>()

    override fun visitElement(element: IrElement) {
        parentChain.push(element)
        element.acceptChildrenVoid(this)
        parentChain.pop()
    }

    override fun visitFunctionAccess(expression: IrFunctionAccessExpression) {
        super.visitFunctionAccess(expression)
        checkFunctionUseSite(expression)
    }

    override fun visitFunctionReference(expression: IrFunctionReference) {
        super.visitFunctionReference(expression)
        checkFunctionUseSite(expression)
    }

    private fun checkFunctionUseSite(expression: IrMemberAccessExpression<IrFunctionSymbol>) {
        val function = expression.symbol.owner
        if (!function.isInline || inlineFunctionUseSiteChecker.isPermitted(expression)) return
        reportError(function, expression)
    }

    private fun reportError(function: IrFunction, expression: IrExpression) {
        val message = buildString {
            append("The following element references ").append(function.visibility).append(" inline ")
            append(
                when (function) {
                    is IrSimpleFunction -> if (function.isAccessor) "property accessor" else "function"
                    is IrConstructor -> "constructor"
                    else -> /* unexpected, but */ function::class.java.simpleName
                }
            )
            append(" ").append(function.name.asString())
        }

        reportError(file, expression, message, parentChain)
    }
}
