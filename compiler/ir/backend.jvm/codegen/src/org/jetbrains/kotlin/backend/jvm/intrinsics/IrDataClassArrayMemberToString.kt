/*
 * Copyright 2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.intrinsics

import org.jetbrains.kotlin.backend.jvm.codegen.BlockInfo
import org.jetbrains.kotlin.backend.jvm.codegen.ExpressionCodegen
import org.jetbrains.kotlin.backend.jvm.codegen.PromisedValue
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.util.isPrimitiveArray

object IrDataClassArrayMemberToString : IntrinsicMethod() {
    override fun invoke(expression: IrFunctionAccessExpression, codegen: ExpressionCodegen, data: BlockInfo): PromisedValue =
        with(codegen) {
            val arrayType = expression.getValueArgument(0)!!.type
            val asmArrayType = codegen.typeMapper.mapType(arrayType)
            gen(expression.getValueArgument(0)!!, asmArrayType, arrayType, data)
            val toStringArgumentDescriptor = if (arrayType.isPrimitiveArray()) asmArrayType.descriptor else "[Ljava/lang/Object;"
            mv.invokestatic("java/util/Arrays", "toString", "($toStringArgumentDescriptor)Ljava/lang/String;", false)
            return expression.onStack
        }
}
