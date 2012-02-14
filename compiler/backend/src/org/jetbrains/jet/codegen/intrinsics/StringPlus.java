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
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.InstructionAdapter;

import java.util.List;

/**
 * @author alex.tkachman
 */
public class StringPlus implements IntrinsicMethod {
    @Override
    public StackValue generate(ExpressionCodegen codegen, InstructionAdapter v, Type expectedType, PsiElement element, List<JetExpression> arguments, StackValue receiver) {
        if(receiver == null || receiver == StackValue.none()) {
            codegen.gen(arguments.get(0)).put(JetTypeMapper.JL_STRING_TYPE, v);
            codegen.gen(arguments.get(1)).put(JetTypeMapper.TYPE_OBJECT, v);
        }
        else {
            receiver.put(JetTypeMapper.JL_STRING_TYPE, v);
            codegen.gen(arguments.get(0)).put(JetTypeMapper.TYPE_OBJECT, v);
        }
        v.invokestatic("jet/runtime/Intrinsics", "stringPlus", "(Ljava/lang/String;Ljava/lang/Object;)Ljava/lang/String;");
        return StackValue.onStack(JetTypeMapper.JL_STRING_TYPE);
    }
}
