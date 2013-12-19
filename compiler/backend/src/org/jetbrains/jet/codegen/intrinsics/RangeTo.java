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
import org.jetbrains.jet.lang.psi.JetBinaryExpression;
import org.jetbrains.jet.lang.psi.JetExpression;

import java.util.List;

public class RangeTo extends IntrinsicMethod {
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
        if (arguments.size() == 1) {
            Type leftType = receiver.type;
            Type rightType = codegen.expressionType(arguments.get(0));
            receiver.put(leftType, v);
            codegen.gen(arguments.get(0), rightType);
            v.invokestatic("jet/runtime/Ranges", "rangeTo",
                           "(" + receiver.type.getDescriptor() + leftType.getDescriptor() + ")" + returnType.getDescriptor());
        }
        else {
            JetBinaryExpression expression = (JetBinaryExpression) element;
            Type leftType = codegen.expressionType(expression.getLeft());
            Type rightType = codegen.expressionType(expression.getRight());
            codegen.gen(expression.getLeft(), leftType);
            codegen.gen(expression.getRight(), rightType);
            v.invokestatic("jet/runtime/Ranges", "rangeTo",
                           "(" + leftType.getDescriptor() + rightType.getDescriptor() + ")" + returnType.getDescriptor());
        }
        return returnType;
    }
}
