/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrSymbolOwner
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid

abstract class IrSymbolVisitor : IrVisitorVoid() {
    abstract fun processSymbol(symbol: IrSymbol)

    override fun visitDeclarationReference(expression: IrDeclarationReference) {
        super.visitDeclarationReference(expression)
        processSymbol(expression.symbol)
    }

    override fun visitInstanceInitializerCall(expression: IrInstanceInitializerCall) {
        super.visitInstanceInitializerCall(expression)
        processSymbol(expression.classSymbol)
    }

    override fun visitReturn(expression: IrReturn) {
        super.visitReturn(expression)
        processSymbol(expression.returnTargetSymbol)
    }

    override fun visitPropertyReference(expression: IrPropertyReference) {
        super.visitPropertyReference(expression)
        expression.field?.let { processSymbol(it) }
        expression.getter?.let { processSymbol(it) }
        expression.setter?.let { processSymbol(it) }
    }

    override fun visitLocalDelegatedPropertyReference(expression: IrLocalDelegatedPropertyReference) {
        super.visitLocalDelegatedPropertyReference(expression)
        processSymbol(expression.delegate)
        processSymbol(expression.getter)
        expression.setter?.let { processSymbol(it) }
    }

    override fun visitCall(expression: IrCall) {
        super.visitCall(expression)
        expression.superQualifierSymbol?.let { processSymbol(it) }
    }
}

class UnboundSymbolsError(message: List<String>) : IllegalStateException(message.joinToString("\n"))

fun checkUnboundSymbols(element: IrElement) {
    val message = mutableListOf<String>()
    val parentChain: MutableList<IrElement> = mutableListOf()
    val visitor = object : IrSymbolVisitor() {
        override fun processSymbol(symbol: IrSymbol) {
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
                if (element is IrSymbolOwner) {
                    processSymbol(element.symbol)
                }
            }
        }
    }
    element.acceptVoid(visitor)
    if (message.isNotEmpty()) {
        throw UnboundSymbolsError(message)
    }
}
