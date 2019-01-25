/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.calls

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.isChar
import org.jetbrains.kotlin.ir.types.isInt
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

fun IrType.isPrimitiveNativelyImplemented(): Boolean = when {
    isInt() -> true
    isChar() -> true
    else -> false
}

class CallsLowering(val context: JsIrBackendContext) : FileLoweringPass {
    private val transformers = listOf(
        NumberOperatorCallsTransformer(context),
        NumberConversionCallsTransformer(context),
        DynamicCallsTransformer(context),
        EqualityAndComparisonCallsTransformer(context),
        PrimitiveContainerMemberCallTransformer(context),
        MethodsOfAnyCallsTransformer(context),
        ReflectionCallsTransformer(context),
        EnumIntrinsicsTransformer(context),
        ExceptionHelperCallsTransformer(context)
    )

    fun shouldSkipCall(call: IrCall): Boolean {
        val function = call.symbol.owner
        val dispathReceiver = function.dispatchReceiverParameter ?: return false
        if (dispathReceiver.type.isPrimitiveNativelyImplemented())
            return true
        return false
    }

    override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitFunction(declaration: IrFunction): IrStatement {
                if (declaration.hasAnnotation(context.intrinsics.doNotIntrinsifyAnnotationSymbol))
                    return declaration
                return super.visitFunction(declaration)
            }

            override fun visitCall(expression: IrCall): IrExpression {
                val call = super.visitCall(expression)
                if (call is IrCall && !shouldSkipCall(call)) {
                    for (transformer in transformers) {
                        val newCall = transformer.transformCall(call)
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
    fun transformCall(call: IrCall): IrExpression
}
