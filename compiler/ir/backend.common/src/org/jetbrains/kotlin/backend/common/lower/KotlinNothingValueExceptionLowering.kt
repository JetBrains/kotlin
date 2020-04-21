/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrSymbolDeclaration
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.isNothing
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

class KotlinNothingValueExceptionLowering(val backendContext: CommonBackendContext) : BodyLoweringPass {
    override fun lower(irBody: IrBody, container: IrDeclaration) {
        irBody.transformChildrenVoid(
            Transformer((container as IrSymbolDeclaration<*>).symbol)
        )
    }

    private inner class Transformer(val parent: IrSymbol) : IrElementTransformerVoid() {
        override fun visitCall(expression: IrCall): IrExpression =
            if (expression.type.isNothing()) {
                // Replace call 'foo' of type 'kotlin.Nothing' with a block:
                //
                //  {
                //      [ call 'foo' with type: 'kotlin.Unit' ]
                //      call ThrowKotlinNothingValueException(): Nothing
                //  }: Nothing
                //
                // Changing type of 'foo' to 'kotlin.Unit' is requires so that the 'ThrowKotlinNothingValueException(): Nothing'
                // is not considered dead code and is not removed.
                // Note that type 'kotlin.Nothing' might be inferred in some cases of projected types.
                // See KT-30330 for an example of such code where call of type 'kotlin.Nothing' terminates and produces some value
                // (although doing so by subverting the type system).
                backendContext.createIrBuilder(parent, expression.startOffset, expression.endOffset).run {
                    irBlock(expression, null, context.irBuiltIns.nothingType) {
                        +super.visitCall(changeTypeToUnit(expression))
                        +irCall(backendContext.ir.symbols.ThrowKotlinNothingValueException)
                    }
                }
            } else {
                super.visitCall(expression)
            }

        private fun changeTypeToUnit(call: IrCall): IrCall =
            IrCallImpl(
                call.startOffset, call.endOffset,
                backendContext.irBuiltIns.unitType,
                call.symbol,
                call.typeArgumentsCount, call.valueArgumentsCount, call.origin, call.superQualifierSymbol
            ).also { newCall ->
                for (i in 0 until call.typeArgumentsCount) {
                    newCall.putTypeArgument(i, call.getTypeArgument(i))
                }
                newCall.dispatchReceiver = call.dispatchReceiver
                newCall.extensionReceiver = call.extensionReceiver
                for (i in 0 until call.valueArgumentsCount) {
                    newCall.putValueArgument(i, call.getValueArgument(i))
                }
            }
    }
}