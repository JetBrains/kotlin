/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.intrinsics

import org.jetbrains.kotlin.backend.jvm.codegen.*
import org.jetbrains.kotlin.backend.jvm.ir.isReifiedTypeParameter
import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.codegen.inline.ReifiedTypeInliner
import org.jetbrains.kotlin.codegen.putReifiedOperationMarkerIfTypeIsReifiedParameter
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.org.objectweb.asm.Type

object EnumValueOf : IntrinsicMethod() {
    override fun invoke(expression: IrFunctionAccessExpression, codegen: ExpressionCodegen, data: BlockInfo) = with(codegen) {
        val type = expression.getTypeArgument(0)!!
        val result = expression.getValueArgument(0)!!.accept(this, data)
            .materializedAt(AsmTypes.JAVA_STRING_TYPE, codegen.context.irBuiltIns.stringType)
        if (type.isReifiedTypeParameter) {
            // Note that the inliner expects exactly the following sequence of instructions.
            // <REIFIED-OPERATIONS-MARKER>
            // ACONST_NULL
            // ALOAD n
            // INVOKESTATIC java/lang/Enum.valueOf...
            val temporary = frameMap.enterTemp(result.type)
            mv.store(temporary, result.type)
            putReifiedOperationMarkerIfTypeIsReifiedParameter(type, ReifiedTypeInliner.OperationKind.ENUM_REIFIED)
            mv.aconst(null)
            mv.load(temporary, result.type)
            val descriptor = Type.getMethodDescriptor(AsmTypes.ENUM_TYPE, AsmTypes.JAVA_CLASS_TYPE, AsmTypes.JAVA_STRING_TYPE)
            mv.invokestatic("java/lang/Enum", "valueOf", descriptor, false)
            frameMap.leaveTemp(result.type)
            MaterialValue(codegen, AsmTypes.ENUM_TYPE, expression.type)
        } else {
            val returnType = typeMapper.mapType(type)
            val descriptor = Type.getMethodDescriptor(returnType, AsmTypes.JAVA_STRING_TYPE)
            mv.invokestatic(returnType.internalName, "valueOf", descriptor, false)
            MaterialValue(codegen, returnType, expression.type)
        }
    }
}

object EnumValues : IntrinsicMethod() {
    override fun invoke(expression: IrFunctionAccessExpression, codegen: ExpressionCodegen, data: BlockInfo) = with(codegen) {
        val type = expression.getTypeArgument(0)!!
        if (type.isReifiedTypeParameter) {
            // Note that the inliner expects exactly the following sequence of instructions.
            // <REIFIED-OPERATIONS-MARKER>
            // ICONST_0
            // ANEWARRAY Ljava/lang/Enum;
            putReifiedOperationMarkerIfTypeIsReifiedParameter(type, ReifiedTypeInliner.OperationKind.ENUM_REIFIED)
            mv.iconst(0)
            mv.newarray(AsmTypes.ENUM_TYPE)
            MaterialValue(codegen, ENUM_ARRAY_TYPE, expression.type)
        } else {
            val enumType = typeMapper.mapType(type)
            val enumArrayType = AsmUtil.getArrayType(enumType)
            val descriptor = Type.getMethodDescriptor(enumArrayType)
            mv.invokestatic(enumType.internalName, "values", descriptor, false)
            MaterialValue(codegen, enumArrayType, expression.type)
        }
    }

    private val ENUM_ARRAY_TYPE = AsmUtil.getArrayType(AsmTypes.ENUM_TYPE)
}

object EnumEntries : IntrinsicMethod() {
    override fun invoke(expression: IrFunctionAccessExpression, codegen: ExpressionCodegen, data: BlockInfo) = with(codegen) {
        val type = expression.getTypeArgument(0)!!
        if (type.isReifiedTypeParameter) {
            // Note that the inliner expects exactly the following sequence of instructions.
            // <REIFIED-OPERATIONS-MARKER>
            // ACONST_NULL
            // CHECKCAST Lkotlin/enums/EnumEntries;
            putReifiedOperationMarkerIfTypeIsReifiedParameter(type, ReifiedTypeInliner.OperationKind.ENUM_REIFIED)
            mv.aconst(null)
            mv.checkcast(AsmTypes.ENUM_ENTRIES)
            MaterialValue(codegen, AsmTypes.ENUM_ENTRIES, expression.type)
        } else {
            val getField = generateExternalEntriesForEnumTypeIfNeeded(type, codegen.classCodegen)
            if (getField != null) {
                mv.visitFieldInsn(getField.opcode, getField.owner, getField.name, getField.desc)
            } else {
                mv.invokestatic(typeMapper.mapType(type).internalName, "getEntries", Type.getMethodDescriptor(AsmTypes.ENUM_ENTRIES), false)
            }
            MaterialValue(codegen, AsmTypes.ENUM_ENTRIES, expression.type)
        }
    }
}
