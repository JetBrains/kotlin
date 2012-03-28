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
import org.jetbrains.jet.codegen.ExpressionCodegen;
import org.jetbrains.jet.codegen.GenerationState;
import org.jetbrains.jet.codegen.StackValue;
import org.jetbrains.jet.lang.psi.JetBinaryExpression;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.InstructionAdapter;

import java.util.List;

/**
 * @author yole
 * @author alex.tkachman
 */
public class UpTo implements IntrinsicMethod {
    private boolean forward;

    public UpTo(boolean forward) {
        this.forward = forward;
    }

    @Override
    public StackValue generate(ExpressionCodegen codegen, InstructionAdapter v, Type expectedType, PsiElement element, List<JetExpression> arguments, StackValue receiver, @NotNull GenerationState state) {
        if(arguments.size()==1) {
            final Type leftType = receiver.type;
            final Type rightType = codegen.expressionType(arguments.get(0));
            receiver.put(Type.INT_TYPE, v);
            codegen.gen(arguments.get(0), rightType);
            v.invokestatic("jet/runtime/Ranges", forward ? "upTo" : "downTo", "(" + receiver.type.getDescriptor() + leftType.getDescriptor() + ")" + expectedType.getDescriptor());
            return StackValue.onStack(expectedType);
        }
        else {
            JetBinaryExpression expression = (JetBinaryExpression) element;
            final Type leftType = codegen.expressionType(expression.getLeft());
            final Type rightType = codegen.expressionType(expression.getRight());
//            if (JetTypeMapper.isIntPrimitive(leftType)) {
                codegen.gen(expression.getLeft(), leftType);
                codegen.gen(expression.getRight(), rightType);
                v.invokestatic("jet/runtime/Ranges", forward ? "upTo" : "downTo", "(" + leftType.getDescriptor() + rightType.getDescriptor() + ")" + expectedType.getDescriptor());
                return StackValue.onStack(expectedType);
//            }
//            else {
//                throw new UnsupportedOperationException("ranges are only supported for int objects");
//            }
        }
    }
}
