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

import com.intellij.psi.PsiElement
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter
import org.jetbrains.kotlin.codegen.ExpressionCodegen
import org.jetbrains.kotlin.codegen.StackValue
import org.jetbrains.kotlin.psi.JetExpression
import org.jetbrains.org.objectweb.asm.Opcodes.*
import org.jetbrains.kotlin.codegen.AsmUtil.isPrimitive
import org.jetbrains.kotlin.codegen.AsmUtil.numberFunctionOperandType

public class BinaryOp(private val opcode: Int) : IntrinsicMethod() {

    override fun generateImpl(codegen: ExpressionCodegen, v: InstructionAdapter, returnType: Type, element: PsiElement?, arguments: List<JetExpression>, receiver: StackValue): Type {
        assert(isPrimitive(returnType)) { "Return type of BinaryOp intrinsic should be of primitive type : " + returnType }

        val operandType = numberFunctionOperandType(returnType)

        if (arguments.size() == 1) {
            // Intrinsic is called as an ordinary function
            if (receiver != StackValue.none()) {
                receiver.put(operandType, v)
            }
            codegen.gen(arguments.get(0), if (shift()) Type.INT_TYPE else operandType)
        }
        else {
            codegen.gen(arguments.get(0), operandType)
            codegen.gen(arguments.get(1), if (shift()) Type.INT_TYPE else operandType)
        }
        v.visitInsn(returnType.getOpcode(opcode))
        return returnType
    }

    private fun shift(): Boolean {
        return opcode == ISHL || opcode == ISHR || opcode == IUSHR
    }
}
