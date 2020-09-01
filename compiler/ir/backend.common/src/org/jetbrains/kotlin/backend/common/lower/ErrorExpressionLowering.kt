/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.DeclarationTransformer
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrErrorDeclaration
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrCompositeImpl
import org.jetbrains.kotlin.ir.types.IrErrorType
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

abstract class ErrorDeclarationLowering : DeclarationTransformer {
    abstract fun transformErrorDeclaration(declaration: IrErrorDeclaration): IrDeclaration
    override fun transformFlat(declaration: IrDeclaration): List<IrDeclaration>? {
        if (declaration is IrErrorDeclaration) return listOf(transformErrorDeclaration(declaration))
        return null
    }
}

abstract class ErrorExpressionLowering(context: CommonBackendContext) : BodyLoweringPass {

    protected val nothingType = context.irBuiltIns.nothingType

    abstract fun transformErrorExpression(expression: IrExpression, nodeKind: String): IrExpression

    override fun lower(irBody: IrBody, container: IrDeclaration) {
        irBody.transformChildrenVoid(object : IrElementTransformerVoid() {

            override fun visitErrorExpression(expression: IrErrorExpression): IrExpression {
                return transformErrorExpression(expression, "Error Expression")
            }

            override fun visitErrorCallExpression(expression: IrErrorCallExpression): IrExpression {
                expression.transformChildrenVoid(this)
                val statements = mutableListOf<IrExpression>().apply {
                    expression.explicitReceiver?.let { add(it) }
                    addAll(expression.arguments)
                    add(transformErrorExpression(expression, "Error Call"))
                }
                return expression.run {
                    IrCompositeImpl(startOffset, endOffset, nothingType, null, statements)
                }
            }

            override fun visitTypeOperator(expression: IrTypeOperatorCall): IrExpression {
                expression.transformChildrenVoid(this)
                if (expression.typeOperand is IrErrorType) {
                    return expression.run {
                        IrCompositeImpl(
                            startOffset, endOffset, nothingType, null, listOf(
                                argument, transformErrorExpression(this, "Error Type")
                            )
                        )
                    }
                }

                return expression
            }
        })
    }
}