/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.expressions.impl.IrGetObjectValueImpl
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.symbols.IrFieldSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol

class VariablesCollapsingLowering(context: JsIrBackendContext) : BodyLoweringPass {
    private val transformer = VariablesCollapsingLoweringVisitor(context)

    override fun lower(irBody: IrBody, container: IrDeclaration) {
        irBody.transformChildrenVoid(transformer)
    }
}

private class VariablesCollapsingLoweringVisitor(val context: JsIrBackendContext) : IrElementTransformerVoidWithContext() {
    private val unitType = context.irBuiltIns.unitType
    private val unitValue get() = IrGetObjectValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, unitType, context.irBuiltIns.unitClass)

    override fun visitVariable(declaration: IrVariable): IrStatement {
        return if (declaration.symbol.getReplacement(context) != null) {
            unitValue
        } else {
            super.visitVariable(declaration)
        }
    }

    override fun visitSetValue(expression: IrSetValue): IrExpression {
        return if (expression.symbol.getReplacement(context) != null) {
            unitValue
        } else {
            super.visitSetValue(expression)
        }
    }

    override fun visitSetField(expression: IrSetField): IrExpression {
        return if (expression.symbol.getReplacement(context) != null) {
            unitValue
        } else {
            super.visitSetField(expression)
        }
    }

    override fun visitGetField(expression: IrGetField): IrExpression {
        return expression.collapse(context).apply {
            super.visitExpression(this)
        }
    }

    override fun visitGetValue(expression: IrGetValue): IrExpression {
        return expression.collapse(context).apply {
            super.visitExpression(this)
        }
    }
}

sealed interface CollapsableElements {
    data class CollapsableValue(val symbol: IrValueSymbol) : CollapsableElements
    data class CollapsableField(val symbol: IrFieldSymbol) : CollapsableElements

    fun collapse(initial: IrExpression, context: JsIrBackendContext): IrExpression {
        var result: IrExpression = initial;

        while (result is IrGetValue || result is IrGetField) {
            val symbol = when (result) {
                is IrGetValue -> result.symbol
                is IrGetField -> result.symbol
                else -> break
            }
            val valueReplacement: IrExpression = symbol.getReplacement(context) ?: break;
            result = valueReplacement;
        }

        return result;
    }
}

fun IrGetValue.collapse(context: JsIrBackendContext): IrExpression {
    return CollapsableElements.CollapsableValue(symbol).collapse(this, context)
}

fun IrGetField.collapse(context: JsIrBackendContext): IrExpression {
    return CollapsableElements.CollapsableField(symbol).collapse(this, context)
}


private fun IrSymbol.getReplacement(context: JsIrBackendContext): IrExpression? {
    return context.optimizations.collapsableVariablesValues[this]
}
