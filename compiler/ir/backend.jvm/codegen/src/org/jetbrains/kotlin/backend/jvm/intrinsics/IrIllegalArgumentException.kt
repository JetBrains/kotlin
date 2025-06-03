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
import org.jetbrains.kotlin.backend.jvm.codegen.ExpressionCodegen
import org.jetbrains.kotlin.backend.jvm.codegen.MaterialValue
import org.jetbrains.kotlin.backend.jvm.codegen.PromisedValue
import org.jetbrains.kotlin.backend.jvm.codegen.materializeAt
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.resolve.jvm.AsmTypes.JAVA_STRING_TYPE
import org.jetbrains.org.objectweb.asm.Type

object IrIllegalArgumentException : IntrinsicMethod() {
    private val exceptionTypeDescriptor = Type.getType(IllegalArgumentException::class.java)!!

    override fun invoke(expression: IrFunctionAccessExpression, codegen: ExpressionCodegen, data: BlockInfo): PromisedValue? {
        val v = codegen.mv
        with(codegen) { expression.markLineNumber(startOffset = true) }
        v.anew(exceptionTypeDescriptor)
        v.dup()
        val argument = expression.arguments.single()!!
        argument.accept(codegen, data).materializeAt(JAVA_STRING_TYPE, argument.type)
        v.invokespecial(exceptionTypeDescriptor.internalName, "<init>", Type.getMethodDescriptor(Type.VOID_TYPE, JAVA_STRING_TYPE), false)
        v.athrow()
        return MaterialValue(codegen, Type.VOID_TYPE, expression.type)
    }
}
