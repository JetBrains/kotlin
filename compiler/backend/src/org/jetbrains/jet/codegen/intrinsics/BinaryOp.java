/*
 * Copyright 2010-2012 JetBrains s.r.o.
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
import org.jetbrains.jet.codegen.state.GenerationState;
import org.jetbrains.jet.lang.psi.JetExpression;

import java.util.List;

import static org.jetbrains.asm4.Opcodes.*;
import static org.jetbrains.jet.codegen.AsmUtil.boxType;
import static org.jetbrains.jet.codegen.AsmUtil.unboxType;

public class BinaryOp implements IntrinsicMethod {
    private final int opcode;

    public BinaryOp(int opcode) {
        this.opcode = opcode;
    }

    @Override
    public StackValue generate(
            ExpressionCodegen codegen,
            InstructionAdapter v,
            @NotNull Type expectedType,
            PsiElement element,
            List<JetExpression> arguments,
            StackValue receiver,
            @NotNull GenerationState state
    ) {
        boolean nullable = expectedType.getSort() == Type.OBJECT;
        if (nullable) {
            expectedType = unboxType(expectedType);
        }
        if (arguments.size() == 1) {
            // Intrinsic is called as an ordinary function
            if (receiver != null) {
                receiver.put(expectedType, v);
            }
            codegen.gen(arguments.get(0), shift() ? Type.INT_TYPE : expectedType);
        }
        else {
            codegen.gen(arguments.get(0), expectedType);
            codegen.gen(arguments.get(1), shift() ? Type.INT_TYPE : expectedType);
        }
        v.visitInsn(expectedType.getOpcode(opcode));

        if (nullable) {
            StackValue.onStack(expectedType).put(expectedType = boxType(expectedType), v);
        }
        return StackValue.onStack(expectedType);
    }

    private boolean shift() {
        return opcode == ISHL || opcode == ISHR || opcode == IUSHR;
    }
}
