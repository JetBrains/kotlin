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
import org.jetbrains.jet.codegen.ExpressionCodegen;
import org.jetbrains.jet.codegen.StackValue;
import org.jetbrains.jet.lang.psi.JetBinaryExpression;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.org.objectweb.asm.Type;
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter;

import java.util.List;

import static org.jetbrains.org.objectweb.asm.Type.*;

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
        v.anew(returnType);
        v.dup();

        Type type;
        if (arguments.size() == 1) {
            assert receiver instanceof StackValue.CallReceiver :
                    "Receiver in an intrinsic qualified expression should be CallReceiver: " + receiver + " on " + element.getText();
            type = parameterType(receiver.type, codegen.expressionType(arguments.get(0)));
            receiver.put(type, v);
            codegen.gen(arguments.get(0), type);
        }
        else {
            JetBinaryExpression expression = (JetBinaryExpression) element;
            type = parameterType(codegen.expressionType(expression.getLeft()), codegen.expressionType(expression.getRight()));
            codegen.gen(expression.getLeft(), type);
            codegen.gen(expression.getRight(), type);
        }

        v.invokespecial(returnType.getInternalName(), "<init>", Type.getMethodDescriptor(Type.VOID_TYPE, type, type), false);

        return returnType;
    }

    @NotNull
    private static Type parameterType(@NotNull Type leftType, @NotNull Type rightType) {
        int left = leftType.getSort();
        int right = rightType.getSort();
        if (left == DOUBLE || right == DOUBLE) {
            return DOUBLE_TYPE;
        }
        else if (left == FLOAT || right == FLOAT) {
            return FLOAT_TYPE;
        }
        else if (left == LONG || right == LONG) {
            return LONG_TYPE;
        }
        else if (left == INT || right == INT) {
            return INT_TYPE;
        }
        else if (left == SHORT || right == SHORT) {
            return SHORT_TYPE;
        }
        else if (left == CHAR || right == CHAR) {
            return CHAR_TYPE;
        }
        else if (left == BYTE || right == BYTE) {
            return BYTE_TYPE;
        }
        else {
            throw new IllegalStateException("RangeTo intrinsic can only work for primitive types: " + leftType + ", " + rightType);
        }
    }
}
