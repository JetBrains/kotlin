/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.JsStatementOrigins
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.backend.js.constructorFactory
import org.jetbrains.kotlin.ir.backend.js.utils.hasStrictSignature
import org.jetbrains.kotlin.ir.backend.js.utils.jsConstructorReference
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

/**
 * Lowers constructor usages to support ES classes.
 */
class ES6ConstructorCallLowering(val context: JsIrBackendContext) : BodyLoweringPass {

    override fun lower(irBody: IrBody, container: IrDeclaration) {
        if (!context.es6mode) return

        val containerFunction = container as? IrFunction

        irBody.transformChildrenVoid(object : IrElementTransformerVoidWithContext() {
            override fun visitConstructorCall(expression: IrConstructorCall): IrExpression {
                val currentConstructor = expression.symbol.owner
                val irClass = currentConstructor.parentAsClass
                val currentFunction = currentFunction?.irElement as? IrFunction ?: containerFunction

                if (irClass.symbol == context.irBuiltIns.anyClass || currentConstructor.hasStrictSignature(context)) {
                    return super.visitConstructorCall(expression)
                }

                val factoryFunction = currentConstructor.constructorFactory
                    ?: irError("Replacement for the constructor is not found") {
                        withIrEntry("currentConstructor", currentConstructor)
                        withIrEntry("expression", expression)
                    }

                val isDelegatingCall = expression.isSyntheticDelegatingReplacement && currentFunction != null

                val factoryFunctionCall = JsIrBuilder.buildCall(
                    factoryFunction.symbol,
                    origin = if (isDelegatingCall) ES6_DELEGATING_CONSTRUCTOR_REPLACEMENT else JsStatementOrigins.SYNTHESIZED_STATEMENT
                ).apply {
                    copyValueArgumentsFrom(expression, factoryFunction)

                    if (expression.isSyntheticDelegatingReplacement) {
                        currentFunction?.boxParameter?.let {
                            arguments[arguments.lastIndex] = JsIrBuilder.buildGetValue(it.symbol)
                        }
                        if (superQualifierSymbol == null) {
                            dispatchReceiver = JsIrBuilder.buildGetValue(factoryFunction.dispatchReceiverParameter!!.symbol)
                        }
                    } else {
                        dispatchReceiver = irClass.jsConstructorReference(context)
                    }
                }

                return super.visitCall(factoryFunctionCall)
            }
        })
    }
}
