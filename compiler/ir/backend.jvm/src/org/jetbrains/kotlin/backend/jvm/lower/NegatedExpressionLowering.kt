/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.codegen.intrinsics.Not
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

// TODO: This lowering is currently JvmBackend specific because there are multiple
// definitions of the boolean 'not' operator. Once there is only the irBuiltins
// definition this lowering could be shared with other backends.
class NegatedExpressionLowering(val context: JvmBackendContext) : FileLoweringPass {

    companion object {
        fun isNegation(expression: IrExpression, context: JvmBackendContext) : Boolean {
            // TODO: there should be only one representation of the 'not' operator.
            return expression is IrCall &&
                    (expression.symbol == context.irBuiltIns.booleanNotSymbol ||
                            context.state.intrinsics.getIntrinsic(expression.symbol.descriptor) is Not)
        }

        fun negationArgument(call: IrCall) : IrExpression {
            // TODO: there should be only one representation of the 'not' operator.
            // Once there is only the IR definition the negation argument will
            // always be a value argument and this method can be removed.
            return call.dispatchReceiver ?: call.getValueArgument(0)!!
        }
    }

    override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitCall(expression: IrCall): IrExpression {
                expression.transformChildrenVoid(this)
                return if (isNegation(expression, context) && isNegation(negationArgument(expression), context))
                    negationArgument(negationArgument(expression) as IrCall)
                else
                    expression
            }
        })
    }

}