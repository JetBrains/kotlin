/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.backend.jvm.intrinsics

import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.backend.jvm.codegen.*
import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter

object HashCode : IntrinsicMethod() {
    override fun invoke(expression: IrFunctionAccessExpression, codegen: ExpressionCodegen, data: BlockInfo) = with(codegen) {
        val receiver = expression.dispatchReceiver ?: error("No receiver for hashCode: ${expression.render()}")
        val receiverIrType = receiver.type
        val receiverJvmType = typeMapper.mapType(receiverIrType)
        val receiverValue = receiver.accept(this, data).materialized()
        val receiverType = receiverValue.type
        when {
            irFunction.origin == JvmLoweredDeclarationOrigin.INLINE_CLASS_GENERATED_IMPL_METHOD ||
                    irFunction.origin == JvmLoweredDeclarationOrigin.MULTI_FIELD_VALUE_CLASS_GENERATED_IMPL_METHOD ||
                    irFunction.origin == IrDeclarationOrigin.GENERATED_DATA_CLASS_MEMBER ||
                    irFunction.origin == IrDeclarationOrigin.GENERATED_MULTI_FIELD_VALUE_CLASS_MEMBER -> {
                genHashCode(mv, receiverType)
            }
            AsmUtil.isPrimitive(receiverJvmType) -> {
                val boxedType = AsmUtil.boxPrimitiveType(receiverJvmType)
                    ?: throw AssertionError("Primitive type expected: $receiverJvmType")
                receiverValue.materializeAt(receiverJvmType, receiverIrType)
                mv.invokestatic(boxedType.internalName, "hashCode", Type.getMethodDescriptor(Type.INT_TYPE, receiverJvmType), false)
            }
            else -> {
                receiverValue.materializeAtBoxed(receiverIrType)
                mv.invokevirtual("java/lang/Object", "hashCode", "()I", false)
            }
        }
        MaterialValue(codegen, Type.INT_TYPE, codegen.context.irBuiltIns.intType)
    }

    private fun genHashCode(iv: InstructionAdapter, type: Type) {
        when {
            type.sort == Type.ARRAY -> {
                val elementType = AsmUtil.correctElementType(type)
                if (elementType.sort == Type.OBJECT || elementType.sort == Type.ARRAY) {
                    iv.invokestatic("java/util/Arrays", "hashCode", "([Ljava/lang/Object;)I", false)
                } else {
                    iv.invokestatic("java/util/Arrays", "hashCode", Type.getMethodDescriptor(Type.INT_TYPE, type), false)
                }
            }
            type.sort == Type.OBJECT ->
                iv.invokevirtual("java/lang/Object", "hashCode", "()I", false)
            AsmUtil.isPrimitive(type) ->
                iv.invokestatic(AsmUtil.boxType(type).internalName, "hashCode", Type.getMethodDescriptor(Type.INT_TYPE, type), false)
            else ->
                error("Unsupported type sort: $type")
        }
    }
}
