/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.backend.js.utils.hasStrictSignature
import org.jetbrains.kotlin.ir.backend.js.utils.jsConstructorReference
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

class ES6ConstructorCallLowering(val context: JsIrBackendContext) : BodyLoweringPass {
    private var IrConstructor.constructorFactory by context.mapping.secondaryConstructorToFactory

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

                val factoryFunction = currentConstructor.constructorFactory ?: error("Replacement for the constructor is not found")

                if (expression.isInitCall) {
                    assert(factoryFunction.isInitFunction) { "Expect to have init function replacement" }
                    return JsIrBuilder.buildCall(factoryFunction.symbol).apply {
                        copyValueArgumentsFrom(expression, factoryFunction)
                    }
                }

                val isDelegatingCall =
                    expression.isSyntheticDelegatingReplacement && currentFunction != null && currentFunction.parentAsClass != irClass

                val factoryFunctionCall = when {
                    isDelegatingCall -> JsIrBuilder.buildCall(
                        factoryFunction.symbol,
                        superQualifierSymbol = irClass.symbol,
                        origin = ES6_DELEGATING_CONSTRUCTOR_REPLACEMENT
                    )
                    else -> JsIrBuilder.buildCall(factoryFunction.symbol)
                }

                factoryFunctionCall.copyValueArgumentsFrom(expression, factoryFunction)

                if (!expression.isSyntheticDelegatingReplacement) {
                    factoryFunctionCall.dispatchReceiver = irClass.jsConstructorReference(context)
                } else {
                    factoryFunctionCall.dispatchReceiver = JsIrBuilder.buildGetValue(factoryFunction.dispatchReceiverParameter!!.symbol)
                    currentFunction?.boxParameter?.let {
                        factoryFunctionCall.putValueArgument(
                            factoryFunctionCall.valueArgumentsCount - 1,
                            JsIrBuilder.buildGetValue(it.symbol)
                        )
                    }
                }

                return super.visitCall(factoryFunctionCall)
            }
        })
    }
}