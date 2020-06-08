/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrBlockImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionReferenceImpl
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

class ProvisionalFunctionExpressionLowering :
    IrElementTransformerVoid(),
    BodyLoweringPass {

    override fun lower(irBody: IrBody, container: IrDeclaration) {
        irBody.transformChildrenVoid(this)
    }

    override fun visitFunctionExpression(expression: IrFunctionExpression): IrExpression {
        expression.transformChildrenVoid(this)

        val startOffset = expression.startOffset
        val endOffset = expression.endOffset
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