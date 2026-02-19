/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.intrinsics

import org.jetbrains.kotlin.backend.jvm.codegen.*
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.org.objectweb.asm.Type

object RangeTo : IntrinsicMethod() {
    override fun invoke(expression: IrFunctionAccessExpression, codegen: ExpressionCodegen, data: BlockInfo): PromisedValue? {
        val signature = codegen.methodSignatureMapper.mapSignatureSkipGeneric(expression.symbol.owner)
        val argType = mapRangeTypeToPrimitiveType(signature.returnType)
        with(codegen) { expression.markLineNumber(startOffset = true) }
        val v = codegen.mv
        v.anew(signature.returnType)
        v.dup()
        val (arg0, arg1) = expression.arguments
        arg0!!.accept(codegen, data).materializeAt(argType, arg0.type)
        arg1!!.accept(codegen, data).materializeAt(argType, arg1.type)
        v.invokespecial(signature.returnType.internalName, "<init>", Type.getMethodDescriptor(Type.VOID_TYPE, argType, argType), false)
        return MaterialValue(codegen, signature.returnType, expression.type)
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
