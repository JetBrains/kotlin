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

import org.jetbrains.kotlin.backend.jvm.codegen.ClassCodegen
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type

object Clone : IntrinsicMethod() {

    private val CLONEABLE_TYPE = Type.getObjectType("java/lang/Cloneable")

    override fun toCallable(
        expression: IrFunctionAccessExpression,
        signature: JvmMethodSignature,
        classCodegen: ClassCodegen
    ): IntrinsicFunction {
        val isSuperCall = expression is IrCall && expression.superQualifierSymbol != null
        val opcode = if (isSuperCall) Opcodes.INVOKESPECIAL else Opcodes.INVOKEVIRTUAL
        val newSignature = signature.newReturnType(AsmTypes.OBJECT_TYPE)
        val argTypes0 = expression.argTypes(classCodegen)
        // Don't upcast receiver to java.lang.Cloneable, since 'clone' is protected in java.lang.Object.
        val argTypes = if (isSuperCall || argTypes0[0] == CLONEABLE_TYPE) listOf(AsmTypes.OBJECT_TYPE) else argTypes0
        return IntrinsicFunction.create(expression, newSignature, classCodegen, argTypes) { mv ->
            mv.visitMethodInsn(opcode, "java/lang/Object", "clone", "()Ljava/lang/Object;", false)
        }
    }
}
