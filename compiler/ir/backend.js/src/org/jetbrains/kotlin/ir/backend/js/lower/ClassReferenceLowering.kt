/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.expressions.IrClassReference
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetClass
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

class ClassReferenceLowering(val context: JsIrBackendContext) : FileLoweringPass {
    private val intrinsics = context.intrinsics

    private fun callGetKClassFromExpression(returnType: IrType, typeArgument: IrType, argument: IrExpression) =
        JsIrBuilder.buildCall(intrinsics.jsGetKClassFromExpression, returnType, listOf(typeArgument)).apply {
            putValueArgument(0, argument)
        }

    private fun callGetKClass(returnType: IrType, typeArgument: IrType, argument: IrExpression) =
        JsIrBuilder.buildCall(intrinsics.jsGetKClass, returnType, listOf(typeArgument)).apply {
            putValueArgument(0, argument)
        }

    private fun callJsClass(type: IrType) =
        JsIrBuilder.buildCall(intrinsics.jsClass, typeArguments = listOf(type))

    override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitGetClass(expression: IrGetClass) =
                callGetKClassFromExpression(
                    returnType = expression.type,
                    typeArgument = expression.argument.type,
                    argument = expression.argument.transform(this, null)
                )

            override fun visitClassReference(expression: IrClassReference) =
                callGetKClass(
                    returnType = expression.type,
                    typeArgument = expression.classType,
                    argument = callJsClass(expression.classType)
                )
        })
    }
}

