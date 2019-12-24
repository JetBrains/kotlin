/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.calls

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

class CallsLowering(val context: JsIrBackendContext) : BodyLoweringPass {
    private val transformers = listOf(
        NumberOperatorCallsTransformer(context),
        NumberConversionCallsTransformer(context),
        EqualityAndComparisonCallsTransformer(context),
        PrimitiveContainerMemberCallTransformer(context),
        MethodsOfAnyCallsTransformer(context),
        ReflectionCallsTransformer(context),
        EnumIntrinsicsTransformer(context),
        ExceptionHelperCallsTransformer(context),
        BuiltInConstructorCalls(context),
        JsonIntrinsics(context)
    )

    override fun lower(irBody: IrBody, container: IrDeclaration) {
        if (container is IrFunction && container.hasAnnotation(context.intrinsics.doNotIntrinsifyAnnotationSymbol)) return

        irBody.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitFunction(declaration: IrFunction): IrStatement {
                if (declaration.hasAnnotation(context.intrinsics.doNotIntrinsifyAnnotationSymbol))
                    return declaration
                return super.visitFunction(declaration)
            }

            override fun visitFunctionAccess(expression: IrFunctionAccessExpression): IrExpression {
                val call = super.visitFunctionAccess(expression)
                if (call is IrFunctionAccessExpression) {
                    for (transformer in transformers) {
                        val newCall = transformer.transformFunctionAccess(call)
                        if (newCall !== call) {
                            return newCall
                        }
                    }
                }
                return call
            }
        })
    }
}

interface CallsTransformer {
    fun transformFunctionAccess(call: IrFunctionAccessExpression): IrExpression
}
