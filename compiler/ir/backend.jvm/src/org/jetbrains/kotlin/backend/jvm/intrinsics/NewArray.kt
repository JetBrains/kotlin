/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.intrinsics

import org.jetbrains.kotlin.backend.jvm.codegen.BlockInfo
import org.jetbrains.kotlin.backend.jvm.codegen.ExpressionCodegen
import org.jetbrains.kotlin.backend.jvm.codegen.PromisedValue
import org.jetbrains.kotlin.codegen.inline.ReifiedTypeInliner
import org.jetbrains.kotlin.codegen.putReifiedOperationMarkerIfTypeIsReifiedParameter
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.types.getArrayElementType
import org.jetbrains.kotlin.ir.types.isArray
import org.jetbrains.kotlin.ir.types.isNullableArray
import org.jetbrains.org.objectweb.asm.Type

object NewArray : IntrinsicMethod() {
    override fun invoke(expression: IrFunctionAccessExpression, codegen: ExpressionCodegen, data: BlockInfo): PromisedValue {
        codegen.gen(expression.getValueArgument(0)!!, Type.INT_TYPE, codegen.context.irBuiltIns.intType, data)
        return with(codegen) {
            val elementIrType = expression.type.getArrayElementType(context.irBuiltIns)
            if (expression.type.isArray() || expression.type.isNullableArray()) {
                putReifiedOperationMarkerIfTypeIsReifiedParameter(elementIrType, ReifiedTypeInliner.OperationKind.NEW_ARRAY)
                mv.newarray(typeMapper.boxType(elementIrType))
            } else {
                mv.newarray(typeMapper.mapType(elementIrType))
            }
            expression.onStack
        }
    }
}
