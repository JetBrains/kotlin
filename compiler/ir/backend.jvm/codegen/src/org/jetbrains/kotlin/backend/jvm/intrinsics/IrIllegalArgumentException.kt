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

import org.jetbrains.kotlin.backend.jvm.codegen.BlockInfo
import org.jetbrains.kotlin.backend.jvm.codegen.ClassCodegen
import org.jetbrains.kotlin.backend.jvm.codegen.ExpressionCodegen
import org.jetbrains.kotlin.codegen.StackValue
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.resolve.jvm.AsmTypes.JAVA_STRING_TYPE
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter

object IrIllegalArgumentException : IntrinsicMethod() {
    val exceptionTypeDescriptor = Type.getType(IllegalArgumentException::class.java)!!

    override fun toCallable(
        expression: IrFunctionAccessExpression,
        signature: JvmMethodSignature,
        classCodegen: ClassCodegen
    ): IntrinsicFunction {
        return object : IntrinsicFunction(expression, signature, classCodegen, listOf(JAVA_STRING_TYPE)) {
            override fun genInvokeInstruction(v: InstructionAdapter) {
                v.invokespecial(
                    exceptionTypeDescriptor.internalName,
                    "<init>",
                    Type.getMethodDescriptor(Type.VOID_TYPE, JAVA_STRING_TYPE),
                    false
                )
                v.athrow()
            }

            override fun invoke(
                v: InstructionAdapter,
                codegen: ExpressionCodegen,
                data: BlockInfo,
                expression: IrFunctionAccessExpression
            ): StackValue {
                with(codegen) { expression.markLineNumber(startOffset = true) }
                v.anew(exceptionTypeDescriptor)
                v.dup()
                return super.invoke(v, codegen, data, expression)
            }
        }
    }
}