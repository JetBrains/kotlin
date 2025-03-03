/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrSymbolOwner
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.IrDynamicType
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid

private abstract class IrSymbolVisitor : IrVisitorVoid() {
    abstract fun processSymbol(element: IrElement, symbol: IrSymbol)

    override fun visitElement(element: IrElement) {
        if (element is IrSymbolOwner) {
            processSymbol(element, element.symbol)
        }
    }

    override fun visitDeclarationReference(expression: IrDeclarationReference) {
        visitElement(expression)
        processSymbol(expression, expression.symbol)
    }

    override fun visitInstanceInitializerCall(expression: IrInstanceInitializerCall) {
        visitElement(expression)
        processSymbol(expression, expression.classSymbol)
    }

    override fun visitReturn(expression: IrReturn) {
        visitElement(expression)
        processSymbol(expression, expression.returnTargetSymbol)
    }

    override fun visitPropertyReference(expression: IrPropertyReference) {
        visitDeclarationReference(expression)
        expression.field?.let { processSymbol(expression, it) }
        expression.getter?.let { processSymbol(expression, it) }
        expression.setter?.let { processSymbol(expression, it) }
    }

    override fun visitLocalDelegatedPropertyReference(expression: IrLocalDelegatedPropertyReference) {
        visitDeclarationReference(expression)
        processSymbol(expression, expression.delegate)
        processSymbol(expression, expression.getter)
        expression.setter?.let { processSymbol(expression, it) }
    }

    override fun visitCall(expression: IrCall) {
        visitDeclarationReference(expression)
        expression.superQualifierSymbol?.let { processSymbol(expression, it) }
    }
}

class UnboundSymbolsError(message: List<String>) : IllegalStateException(message.joinToString("\n"))

fun checkUnboundSymbols(element: IrElement) {
    val message = mutableListOf<String>()
    val parentChain: MutableList<IrElement> = mutableListOf()
    val visitor = object : IrSymbolVisitor() {
        override fun processSymbol(element: IrElement, symbol: IrSymbol) {
            if (element is IrExpression && element.type is IrDynamicType) return

            if (!symbol.isBound) {
                message.add(
                    buildString {
                        appendLine("[IR unbound symbol error] $symbol")
                        parentChain.asReversed().forEachIndexed { index, parent ->
                            append("  ".repeat(index + 1))
                            append("inside ")
                            appendLine(parent.render())
                        }
                    }
                )
            }
        }

        override fun visitElement(element: IrElement) {
            parentChain.temporarilyPushing(element) {
                element.acceptChildrenVoid(this)
                super.visitElement(element)
            }
        }

        override fun visitDeclarationReference(expression: IrDeclarationReference) {
            visitElement(expression)
            processSymbol(expression, expression.symbol)
        }
    }
    element.acceptVoid(visitor)
    if (message.isNotEmpty()) {
        throw UnboundSymbolsError(message)
    }
}
