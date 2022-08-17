/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrFunctionExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrBlockImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionReferenceImpl
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer

class ProvisionalFunctionExpressionLoweringContext(
    val outer: ProvisionalFunctionExpressionLoweringContext? = null,
    val startOffset: Int? = null,
    val endOffset: Int? = null)
class ProvisionalFunctionExpressionLowering :
    IrElementTransformer<ProvisionalFunctionExpressionLoweringContext>,
    BodyLoweringPass {

    override fun lower(irBody: IrBody, container: IrDeclaration) {
        irBody.transformChildren(this, ProvisionalFunctionExpressionLoweringContext())
    }

    override fun visitCall(expression: IrCall, data: ProvisionalFunctionExpressionLoweringContext) = super.visitCall(
        expression,
        ProvisionalFunctionExpressionLoweringContext(
            data,
            expression.startOffset,
            expression.endOffset
        )
    )

    override fun visitVariable(declaration: IrVariable, data: ProvisionalFunctionExpressionLoweringContext) = super.visitVariable(
        declaration,
        ProvisionalFunctionExpressionLoweringContext(
            data,
            declaration.startOffset,
            declaration.endOffset
        )
    )

    override fun visitFunctionExpression(expression: IrFunctionExpression, data: ProvisionalFunctionExpressionLoweringContext): IrElement {
        expression.transformChildren(this, ProvisionalFunctionExpressionLoweringContext(data))

        val startOffset = data.startOffset ?: expression.startOffset
        val endOffset = data.endOffset ?: expression.endOffset
        val type = expression.type
        val origin = expression.origin
        val function = expression.function

        return IrBlockImpl(
            startOffset, endOffset, type, origin,
            listOf(
                function,
                IrFunctionReferenceImpl.fromSymbolOwner(
                    startOffset, endOffset,
                    type,
                    function.symbol,
                    typeArgumentsCount = 0,
                    reflectionTarget = null,
                    origin = origin
                )
            )
        )
    }
}
