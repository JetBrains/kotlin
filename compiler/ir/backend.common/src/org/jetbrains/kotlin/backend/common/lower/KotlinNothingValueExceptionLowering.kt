/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.isNothing
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

class KotlinNothingValueExceptionLowering(
    val backendContext: CommonBackendContext, val skip: (IrDeclaration) -> Boolean = { false }
) : BodyLoweringPass {
    override fun lower(irBody: IrBody, container: IrDeclaration) {
        if (!skip(container)) {
            irBody.transformChildrenVoid(Transformer(container.symbol))
        }
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
                backendContext.createIrBuilder(parent, expression.startOffset, expression.endOffset).run {
                    irBlock(expression, null, context.irBuiltIns.nothingType) {
                        +super.visitCall(expression)
                        +irCall(backendContext.ir.symbols.throwKotlinNothingValueException)
                    }
                }
            } else {
                super.visitCall(expression)
            }
    }
}