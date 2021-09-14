/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.intrinsics

import org.jetbrains.kotlin.backend.jvm.codegen.BlockInfo
import org.jetbrains.kotlin.backend.jvm.codegen.ExpressionCodegen
import org.jetbrains.kotlin.backend.jvm.codegen.PromisedValue
import org.jetbrains.kotlin.backend.jvm.codegen.materialize
import org.jetbrains.kotlin.backend.jvm.mapping.mapTypeAsDeclaration
import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.ir.expressions.IrClassReference
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.expressions.IrGetClass
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.isNullablePrimitiveType
import org.jetbrains.kotlin.ir.types.isPrimitiveType
import org.jetbrains.kotlin.ir.util.isTypeParameter
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.org.objectweb.asm.Type

object GetJavaPrimitiveType : IntrinsicMethod() {

    override fun invoke(expression: IrFunctionAccessExpression, codegen: ExpressionCodegen, data: BlockInfo): PromisedValue? {
        val receiver = expression.extensionReceiver ?: return null

        val argumentType =
            when (receiver) {
                is IrGetClass -> receiver.argument.type
                is IrClassReference -> receiver.classType
                else -> return null
            }

        if (argumentType.isTypeParameter()) return null

        val argumentAsmType = codegen.typeMapper.mapTypeAsDeclaration(argumentType)

        val isPrimitiveTypeOrWrapper =
            argumentType.isPrimitiveType() ||
                    argumentType.isNullablePrimitiveType() ||
                    !argumentType.isInlineClassType() && argumentAsmType.isVoidOrPrimitiveWrapper()

        return when (receiver) {
            is IrGetClass -> {
                if (!isPrimitiveTypeOrWrapper) return null

                val argumentValue = receiver.argument.accept(codegen, data)
                argumentValue.materialize()
                AsmUtil.pop(codegen.mv, argumentValue.type)
                putPrimitiveType(codegen, argumentAsmType)

                with(codegen) { expression.onStack }
            }

            is IrClassReference -> {
                if (!isPrimitiveTypeOrWrapper) {
                    codegen.mv.aconst(null)
                } else {
                    putPrimitiveType(codegen, argumentAsmType)
                }

                with(codegen) { expression.onStack }
            }

            else ->
                throw AssertionError("IrGetClass or IrClassReference expected: ${receiver.render()}")
        }
    }

    private fun putPrimitiveType(codegen: ExpressionCodegen, type: Type) {
        codegen.mv.getstatic(AsmUtil.boxType(type).internalName, "TYPE", "Ljava/lang/Class;")
    }

    private fun IrType.isInlineClassType(): Boolean {
        return (classOrNull ?: return false).owner.isInline
    }

    private fun Type.isVoidOrPrimitiveWrapper(): Boolean =
        this == AsmTypes.VOID_WRAPPER_TYPE || AsmUtil.unboxPrimitiveTypeOrNull(this) != null
}