/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.intrinsics

import org.jetbrains.kotlin.backend.jvm.codegen.BlockInfo
import org.jetbrains.kotlin.backend.jvm.codegen.ExpressionCodegen
import org.jetbrains.kotlin.backend.jvm.codegen.PromisedValue
import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.codegen.inline.ReifiedTypeInliner
import org.jetbrains.kotlin.codegen.putReifiedOperationMarkerIfTypeIsReifiedParameter
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.types.getArrayElementType
import org.jetbrains.kotlin.ir.types.isArray
import org.jetbrains.kotlin.ir.types.isNullableArray
import org.jetbrains.kotlin.load.kotlin.TypeMappingMode
import org.jetbrains.org.objectweb.asm.Type

object NewVArray : IntrinsicMethod() {
    override fun invoke(expression: IrFunctionAccessExpression, codegen: ExpressionCodegen, data: BlockInfo): PromisedValue {
        return with(codegen) {
            mv.anew(Type.getObjectType(vArrayWrapperType))
            mv.dup()
            gen(expression.getValueArgument(0)!!, Type.INT_TYPE, codegen.context.irBuiltIns.intType, data)
            val elementIrType = expression.type.getArrayElementType(context.irBuiltIns)
            if (expression.type.isArray() || expression.type.isNullableArray()) {
                putReifiedOperationMarkerIfTypeIsReifiedParameter(elementIrType, ReifiedTypeInliner.OperationKind.NEW_ARRAY)
                mv.newarray(typeMapper.boxType(elementIrType))
            } else {
                mv.newarray(typeMapper.mapType(elementIrType))
            }
            mv.aconst(AsmUtil.getArrayType(codegen.typeMapper.mapType(expression.getTypeArgument(0)!!, TypeMappingMode.CLASS_DECLARATION)))
            mv.invokespecial(vArrayWrapperType, "<init>", "(Ljava/lang/Object;Ljava/lang/Class;)V", false)
            expression.onStack
        }
    }
}
