/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.codegen.intrinsics

import org.jetbrains.kotlin.codegen.AsmUtil.numberFunctionOperandType
import org.jetbrains.kotlin.codegen.Callable
import org.jetbrains.kotlin.codegen.CallableMethod
import org.jetbrains.org.objectweb.asm.Opcodes.ISHL
import org.jetbrains.org.objectweb.asm.Opcodes.ISHR
import org.jetbrains.org.objectweb.asm.Opcodes.IUSHR
import org.jetbrains.org.objectweb.asm.Type

public class BinaryOp(private val opcode: Int) : IntrinsicMethod() {

    private fun shift(): Boolean {
        return opcode == ISHL || opcode == ISHR || opcode == IUSHR
    }

    override fun toCallable(method: CallableMethod): Callable {
        val returnType = method.returnType
        assert(method.getValueParameters().size() == 1)
        val operandType = numberFunctionOperandType(returnType)
        val paramType = if (shift()) Type.INT_TYPE else operandType

        return createBinaryIntrinsicCallable(operandType, paramType, operandType) {
            v -> v.visitInsn(returnType.getOpcode(opcode))
        }
    }
}
