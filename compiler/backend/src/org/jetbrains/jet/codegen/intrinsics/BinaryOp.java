/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.codegen.intrinsics;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.asm4.Type;
import org.jetbrains.asm4.commons.InstructionAdapter;
import org.jetbrains.jet.codegen.ExpressionCodegen;
import org.jetbrains.jet.codegen.StackValue;
import org.jetbrains.jet.lang.psi.JetExpression;

import java.util.List;

import static org.jetbrains.asm4.Opcodes.*;
import static org.jetbrains.jet.codegen.AsmUtil.isPrimitive;
import static org.jetbrains.jet.codegen.AsmUtil.numberFunctionOperandType;

public class BinaryOp extends IntrinsicMethod {
    private final int opcode;

    public BinaryOp(int opcode) {
        this.opcode = opcode;
    }

    @NotNull
    @Override
    public Type generateImpl(
            @NotNull ExpressionCodegen codegen,
            @NotNull InstructionAdapter v,
            @NotNull Type returnType,
            PsiElement element,
            List<JetExpression> arguments,
            StackValue receiver
    ) {
        assert isPrimitive(returnType) : "Return type of BinaryOp intrinsic should be of primitive type : " + returnType;

        Type operandType = numberFunctionOperandType(returnType);

        if (arguments.size() == 1) {
            // Intrinsic is called as an ordinary function
            if (receiver != null) {
                receiver.put(operandType, v);
            }
            codegen.gen(arguments.get(0), shift() ? Type.INT_TYPE : operandType);
        }
        else {
            codegen.gen(arguments.get(0), operandType);
            codegen.gen(arguments.get(1), shift() ? Type.INT_TYPE : operandType);
        }
        v.visitInsn(returnType.getOpcode(opcode));
        return returnType;
    }

    private boolean shift() {
        return opcode == ISHL || opcode == ISHR || opcode == IUSHR;
    }
}
