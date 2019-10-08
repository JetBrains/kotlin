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

import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.codegen.AsmUtil.numberFunctionOperandType
import org.jetbrains.kotlin.codegen.StackValue
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.types.isChar
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature
import org.jetbrains.org.objectweb.asm.Opcodes.*
import org.jetbrains.org.objectweb.asm.Type

class BinaryOp(private val opcode: Int) : IntrinsicMethod() {
    private fun shift(): Boolean =
        opcode == ISHL || opcode == ISHR || opcode == IUSHR

    override fun toCallable(
        expression: IrFunctionAccessExpression,
        signature: JvmMethodSignature,
        context: JvmBackendContext
    ): IrIntrinsicFunction {
        val returnType = signature.returnType
        val intermediateResultType = numberFunctionOperandType(returnType)
        val argTypes = if (!expression.symbol.owner.parentAsClass.defaultType.isChar()) {
            listOf(intermediateResultType, if (shift()) Type.INT_TYPE else intermediateResultType)
        } else {
            listOf(Type.CHAR_TYPE, signature.valueParameters[0].asmType)
        }

        return IrIntrinsicFunction.create(expression, signature, context, argTypes) {
            it.visitInsn(returnType.getOpcode(opcode))
            StackValue.coerce(intermediateResultType, returnType, it)
        }
    }
}
