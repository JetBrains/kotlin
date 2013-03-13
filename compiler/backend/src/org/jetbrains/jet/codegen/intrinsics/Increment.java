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
import org.jetbrains.jet.codegen.state.GenerationState;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.psi.JetParenthesizedExpression;
import org.jetbrains.jet.lang.psi.JetReferenceExpression;

import java.util.List;

import static org.jetbrains.jet.codegen.AsmUtil.*;

public class Increment implements IntrinsicMethod {
    private final int myDelta;

    public Increment(int delta) {
        myDelta = delta;
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
        if (arguments.size() > 0) {
            JetExpression operand = arguments.get(0);
            while (operand instanceof JetParenthesizedExpression) {
                operand = ((JetParenthesizedExpression) operand).getExpression();
            }
            if (operand instanceof JetReferenceExpression) {
                int index = codegen.indexOfLocal((JetReferenceExpression) operand);
                if (index >= 0 && isIntPrimitive(expectedType)) {
                    return StackValue.preIncrement(index, myDelta);
                }
            }
            StackValue value = codegen.genQualified(receiver, operand);
            value.dupReceiver(v);
            value.dupReceiver(v);

            value.put(expectedType, v);
            value.store(genIncrement(expectedType, myDelta, v), v);
            value.put(expectedType, v);
        }
        else {
            receiver.put(expectedType, v);
            StackValue.coerce(genIncrement(expectedType, myDelta, v), expectedType, v);
        }
        return StackValue.onStack(expectedType);
    }
}
