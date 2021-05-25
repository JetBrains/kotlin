/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrBlockImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionReferenceImpl
import org.jetbrains.kotlin.ir.util.remapTypes
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

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
                IrFunctionReferenceImpl(
                    startOffset, endOffset, type,
                    function.symbol,
                    typeArgumentsCount = 0,
                    valueArgumentsCount = function.valueParameters.size,
                    reflectionTarget = null,
                    origin = origin
                )
            )
        )
    }
}