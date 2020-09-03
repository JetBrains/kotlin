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
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type

// TODO Implement hashCode on primitive types as a lowering.
object HashCode : IntrinsicMethod() {
    override fun invoke(expression: IrFunctionAccessExpression, codegen: ExpressionCodegen, data: BlockInfo) = with(codegen) {
        val receiver = expression.dispatchReceiver ?: error("No receiver for hashCode: ${expression.render()}")
        val result = receiver.accept(this, data).materialized()
        val target = context.state.target
        when {
            irFunction.origin == JvmLoweredDeclarationOrigin.INLINE_CLASS_GENERATED_IMPL_METHOD ||
                    irFunction.origin == IrDeclarationOrigin.GENERATED_DATA_CLASS_MEMBER ->
                AsmUtil.genHashCode(mv, mv, result.type, target)
            target == JvmTarget.JVM_1_6 || !AsmUtil.isPrimitive(result.type) -> {
                result.materializeAtBoxed(receiver.type)
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Object", "hashCode", "()I", false)
            }
            else -> {
                val boxedType = AsmUtil.boxType(result.type)
                mv.visitMethodInsn(
                    Opcodes.INVOKESTATIC,
                    boxedType.internalName,
                    "hashCode",
                    Type.getMethodDescriptor(Type.INT_TYPE, result.type),
                    false
                )
            }
        }
        MaterialValue(codegen, Type.INT_TYPE, codegen.context.irBuiltIns.intType)
    }
}
