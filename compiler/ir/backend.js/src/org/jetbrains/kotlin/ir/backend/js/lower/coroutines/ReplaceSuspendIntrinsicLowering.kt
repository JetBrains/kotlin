/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.coroutines

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.utils.compileSuspendAsJsGenerator
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrCallableReference
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

class ReplaceSuspendIntrinsicLowering(private val context: JsIrBackendContext) : BodyLoweringPass {
    private val valueParamSizeToItsCreateCoroutineUnintercepted =
        context.intrinsics.createCoroutineUninterceptedGeneratorVersion.groupPerValueParamSize()
    private val valueParamSizeToItsStartCoroutineUninterceptedOrReturn =
        context.intrinsics.startCoroutineUninterceptedOrReturnGeneratorVersion.groupPerValueParamSize()

    private fun Set<IrSimpleFunctionSymbol>.groupPerValueParamSize(): Map<Int, IrSimpleFunctionSymbol> {
        return associateBy { it.owner.valueParameters.size }
    }

    override fun lower(irBody: IrBody, container: IrDeclaration) {
        if (!context.compileSuspendAsJsGenerator) return
        irBody.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitCallableReference(expression: IrCallableReference<*>): IrExpression {
                if (expression.symbol !is IrSimpleFunctionSymbol) return super.visitCallableReference(expression)

                @Suppress("UNCHECKED_CAST")
                val reference = expression as IrCallableReference<IrSimpleFunctionSymbol>

                when (val symbol = reference.symbol) {
                    in context.intrinsics.createCoroutineUnintercepted ->
                        reference.symbol = valueParamSizeToItsCreateCoroutineUnintercepted.getValue(symbol.owner.valueParameters.size)
                    in context.intrinsics.startCoroutineUninterceptedOrReturn ->
                        reference.symbol = valueParamSizeToItsStartCoroutineUninterceptedOrReturn.getValue(symbol.owner.valueParameters.size)
                }

                return super.visitCallableReference(reference)
            }

            override fun visitCall(expression: IrCall): IrExpression {
                when (val symbol = expression.symbol) {
                    in context.intrinsics.createCoroutineUnintercepted ->
                        expression.symbol = valueParamSizeToItsCreateCoroutineUnintercepted.getValue(symbol.owner.valueParameters.size)
                    in context.intrinsics.startCoroutineUninterceptedOrReturn ->
                        expression.symbol = valueParamSizeToItsStartCoroutineUninterceptedOrReturn.getValue(symbol.owner.valueParameters.size)
                }
                return super.visitCall(expression)
            }
        })
    }
}