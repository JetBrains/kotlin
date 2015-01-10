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

package org.jetbrains.kotlin.codegen.intrinsics;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.codegen.ExpressionCodegen;
import org.jetbrains.kotlin.codegen.StackValue;
import org.jetbrains.kotlin.psi.JetExpression;
import org.jetbrains.org.objectweb.asm.Type;
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter;

import java.util.List;

import static org.jetbrains.kotlin.codegen.AsmUtil.comparisonOperandType;

public class CompareTo extends IntrinsicMethod {
    @NotNull
    @Override
    public Type generateImpl(
            @NotNull ExpressionCodegen codegen,
            @NotNull InstructionAdapter v,
            @NotNull Type returnType,
            @Nullable PsiElement element,
            @NotNull List<JetExpression> arguments,
            @NotNull StackValue receiver
    ) {
        JetExpression argument;
        if (arguments.size() == 1) {
            argument = arguments.get(0);
        }
        else if (arguments.size() == 2) {
            receiver = codegen.gen(arguments.get(0));
            argument = arguments.get(1);
        }
        else {
            throw new IllegalStateException("Invalid arguments to compareTo: " + arguments);
        }
        Type type = comparisonOperandType(receiver.type, codegen.expressionType(argument));

        receiver.put(type, v);
        codegen.gen(argument, type);

        if (type == Type.INT_TYPE) {
            v.invokestatic(IntrinsicMethods.INTRINSICS_CLASS_NAME, "compare", "(II)I", false);
        }
        else if (type == Type.LONG_TYPE) {
            v.invokestatic(IntrinsicMethods.INTRINSICS_CLASS_NAME, "compare", "(JJ)I", false);
        }
        else if (type == Type.FLOAT_TYPE) {
            v.invokestatic("java/lang/Float", "compare", "(FF)I", false);
        }
        else if (type == Type.DOUBLE_TYPE) {
            v.invokestatic("java/lang/Double", "compare", "(DD)I", false);
        }
        else {
            throw new UnsupportedOperationException();
        }

        return Type.INT_TYPE;
    }
}
