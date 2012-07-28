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
 * @author alex.tkachman
 */
public class IteratorNext implements IntrinsicMethod {
    @Override
    public StackValue generate(ExpressionCodegen codegen, InstructionAdapter v, @NotNull Type expectedType, PsiElement element, List<JetExpression> arguments, StackValue receiver, @NotNull GenerationState state) {
        String name;
        if (expectedType == Type.CHAR_TYPE)
            name = "Char";
        else if (expectedType == Type.BOOLEAN_TYPE)
            name = "Boolean";
        else if (expectedType == Type.BYTE_TYPE)
            name = "Byte";
        else if (expectedType == Type.SHORT_TYPE)
            name = "Short";
        else if (expectedType == Type.INT_TYPE)
            name = "Int";
        else if (expectedType == Type.LONG_TYPE)
            name = "Long";
        else if (expectedType == Type.FLOAT_TYPE)
            name = "Float";
        else if (expectedType == Type.DOUBLE_TYPE)
            name = "Double";
        else
            throw new UnsupportedOperationException();
        receiver.put(JetTypeMapper.TYPE_OBJECT, v);
        v.invokevirtual("jet/" + name + "Iterator", "next" + name, "()" + expectedType.getDescriptor());
        return StackValue.onStack(expectedType);
    }
}
