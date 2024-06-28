/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.intrinsics

import org.jetbrains.kotlin.backend.jvm.codegen.BlockInfo
import org.jetbrains.kotlin.backend.jvm.codegen.ClassCodegen
import org.jetbrains.kotlin.backend.jvm.codegen.ExpressionCodegen
import org.jetbrains.kotlin.codegen.StackValue
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter

object RangeTo : IntrinsicMethod() {
    override fun toCallable(
        expression: IrFunctionAccessExpression, signature: JvmMethodSignature, classCodegen: ClassCodegen,
    ): IntrinsicFunction {
        val argType = mapRangeTypeToPrimitiveType(signature.returnType)
        return object : IntrinsicFunction(
            expression, signature, classCodegen, listOf(argType) + signature.valueParameters.map { argType }
        ) {
            override fun genInvokeInstruction(v: InstructionAdapter) {
                v.invokespecial(
                    signature.returnType.internalName,
                    "<init>",
                    Type.getMethodDescriptor(Type.VOID_TYPE, argType, argType),
                    false
                )
            }

            override fun invoke(
                v: InstructionAdapter,
                codegen: ExpressionCodegen,
                data: BlockInfo,
                expression: IrFunctionAccessExpression,
            ): StackValue {
                with(codegen) { expression.markLineNumber(startOffset = true) }
                v.anew(signature.returnType)
                v.dup()
                return super.invoke(v, codegen, data, expression)
            }
        }
    }

    private fun mapRangeTypeToPrimitiveType(rangeType: Type): Type {
        val fqName = rangeType.internalName
        return when (fqName.substringAfter("kotlin/ranges/").substringBefore("Range")) {
            "Double" -> Type.DOUBLE_TYPE
            "Float" -> Type.FLOAT_TYPE
            "Long" -> Type.LONG_TYPE
            "Int" -> Type.INT_TYPE
            "Short" -> Type.SHORT_TYPE
            "Char" -> Type.CHAR_TYPE
            "Byte" -> Type.BYTE_TYPE
            else -> throw IllegalStateException("RangeTo intrinsic can only work for primitive types: $fqName")
        }
    }
}
