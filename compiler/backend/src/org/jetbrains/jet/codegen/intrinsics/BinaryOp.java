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
import org.jetbrains.jet.codegen.ExpressionCodegen;
import org.jetbrains.jet.codegen.JetTypeMapper;
import org.jetbrains.jet.codegen.StackValue;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.InstructionAdapter;

import java.util.List;

/**
 * @author yole
 */
public class BinaryOp implements IntrinsicMethod {
    private final int opcode;

    public BinaryOp(int opcode) {
        this.opcode = opcode;
    }

    @Override
    public StackValue generate(ExpressionCodegen codegen, InstructionAdapter v, Type expectedType, PsiElement element, List<JetExpression> arguments, StackValue receiver) {
        boolean nullable = expectedType.getSort() == Type.OBJECT;
        if(nullable) {
            expectedType = JetTypeMapper.unboxType(expectedType);
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

        if(nullable) {
            StackValue.onStack(expectedType).put(expectedType = JetTypeMapper.boxType(expectedType), v);
        }
        return StackValue.onStack(expectedType);
    }

    private boolean shift() {
        return opcode == Opcodes.ISHL || opcode == Opcodes.ISHR || opcode == Opcodes.IUSHR;
    }
}
