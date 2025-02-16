/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.phaser.PhaseDescription
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.declarations.copyAttributes
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrBlockImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionReferenceImpl
import org.jetbrains.kotlin.ir.visitors.IrTransformer

class ProvisionalFunctionExpressionLoweringContext(
    val startOffset: Int? = null,
    val endOffset: Int? = null
)

/**
 * Transforms [IrFunctionExpression] to a block with a local function and a reference to it.
 */
@PhaseDescription(name = "FunctionExpression")
class ProvisionalFunctionExpressionLowering(@Suppress("UNUSED_PARAMETER", "unused") context: CommonBackendContext) :
    IrTransformer<ProvisionalFunctionExpressionLoweringContext>(),
    BodyLoweringPass {

    override fun lower(irBody: IrBody, container: IrDeclaration) {
        irBody.transformChildren(this, ProvisionalFunctionExpressionLoweringContext())
    }

    override fun visitCall(expression: IrCall, data: ProvisionalFunctionExpressionLoweringContext) = super.visitCall(
        expression,
        ProvisionalFunctionExpressionLoweringContext(
            expression.startOffset,
            expression.endOffset
        )
    )

    override fun visitVariable(declaration: IrVariable, data: ProvisionalFunctionExpressionLoweringContext) = super.visitVariable(
        declaration,
        ProvisionalFunctionExpressionLoweringContext(
            declaration.startOffset,
            declaration.endOffset
        )
    )

    override fun visitContainerExpression(expression: IrContainerExpression, data: ProvisionalFunctionExpressionLoweringContext): IrExpression {
        if (expression !is IrReturnableBlock) return super.visitContainerExpression(expression, data)
        return super.visitContainerExpression(expression, ProvisionalFunctionExpressionLoweringContext())
    }

    override fun visitFunctionExpression(expression: IrFunctionExpression, data: ProvisionalFunctionExpressionLoweringContext): IrElement {
        expression.transformChildren(this, ProvisionalFunctionExpressionLoweringContext())

        val startOffset = data.startOffset ?: expression.startOffset
        val endOffset = data.endOffset ?: expression.endOffset
        val type = expression.type
        val origin = expression.origin
        val function = expression.function

        return IrBlockImpl(
            startOffset, endOffset, type, origin,
            listOf(
                function,
                IrFunctionReferenceImpl(
                    startOffset, endOffset, type,
                    function.symbol,
                    typeArgumentsCount = 0,
                    reflectionTarget = null,
                    origin = origin
                ).apply {
                    copyAttributes(expression)
                }
            )
        )
    }
}
