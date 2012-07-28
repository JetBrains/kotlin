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
import org.jetbrains.asm4.Type;
import org.jetbrains.asm4.commons.InstructionAdapter;

import java.util.List;

/**
 * @author yole
 */
public class Concat implements IntrinsicMethod {
    @Override
    public StackValue generate(ExpressionCodegen codegen, InstructionAdapter v, @NotNull Type expectedType, PsiElement element, List<JetExpression> arguments, StackValue receiver, @NotNull GenerationState state) {
        if (receiver == null || receiver == StackValue.none()) {                                                     // LHS + RHS
            codegen.generateStringBuilderConstructor();
            codegen.invokeAppend(arguments.get(0));                                // StringBuilder(LHS)
            codegen.invokeAppend(arguments.get(1));
        }
        else {                                    // LHS.plus(RHS)
            receiver.put(JetTypeMapper.TYPE_OBJECT, v);
            codegen.generateStringBuilderConstructor();
            v.swap();                                                              // StringBuilder LHS
            codegen.invokeAppendMethod(expectedType);  // StringBuilder(LHS)
            codegen.invokeAppend(arguments.get(0));
        }

        v.invokevirtual("java/lang/StringBuilder", "toString", "()Ljava/lang/String;");
        StackValue.onStack(JetTypeMapper.JL_STRING_TYPE).put(expectedType, v);
        return StackValue.onStack(expectedType);
    }
}
