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
import org.jetbrains.jet.codegen.JetTypeMapper;
import org.jetbrains.jet.codegen.StackValue;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.psi.JetParenthesizedExpression;
import org.jetbrains.jet.lang.psi.JetReferenceExpression;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.InstructionAdapter;

import java.util.List;

/**
 * @author yole
 */
public class Increment implements IntrinsicMethod {
    private final int myDelta;

    public Increment(int delta) {
        myDelta = delta;
    }

    @Override
    public StackValue generate(ExpressionCodegen codegen, InstructionAdapter v, Type expectedType, PsiElement element, List<JetExpression> arguments, StackValue receiver, @NotNull GenerationState state) {
        boolean nullable = expectedType.getSort() == Type.OBJECT;
        if(nullable) {
            expectedType = JetTypeMapper.unboxType(expectedType);
        }
        if(arguments.size() > 0) {
            JetExpression operand = arguments.get(0);
            while(operand instanceof JetParenthesizedExpression) {
                operand = ((JetParenthesizedExpression)operand).getExpression();
            }
            if (operand instanceof JetReferenceExpression) {
                final int index = codegen.indexOfLocal((JetReferenceExpression) operand);
                if (index >= 0 && JetTypeMapper.isIntPrimitive(expectedType)) {
                    return StackValue.preIncrement(index, myDelta);
                }
            }
            StackValue value = codegen.genQualified(receiver, operand);
            value. dupReceiver(v);
            value. dupReceiver(v);

            value.put(expectedType, v);
            plusMinus(v, expectedType);
            value.store(v);
            value.put(expectedType, v);
        }
        else {
            receiver.put(expectedType, v);
            plusMinus(v, expectedType);
        }
        return StackValue.onStack(expectedType);
    }

    private void plusMinus(InstructionAdapter v, Type expectedType) {
        if (expectedType == Type.LONG_TYPE) {
            v.lconst(myDelta);
        }
        else if (expectedType == Type.FLOAT_TYPE) {
            v.fconst(myDelta);
        }
        else if (expectedType == Type.DOUBLE_TYPE) {
            v.dconst(myDelta);
        }
        else {
            v.iconst(myDelta);
        }
        v.add(expectedType);
    }
}
