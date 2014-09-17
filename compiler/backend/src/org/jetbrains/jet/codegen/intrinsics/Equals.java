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
import org.jetbrains.org.objectweb.asm.Type;
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter;
import org.jetbrains.jet.codegen.ExpressionCodegen;
import org.jetbrains.jet.codegen.StackValue;
import org.jetbrains.jet.lang.psi.JetCallExpression;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lexer.JetTokens;

import java.util.List;

import static org.jetbrains.jet.codegen.AsmUtil.genEqualsForExpressionsOnStack;
import static org.jetbrains.jet.lang.resolve.java.AsmTypeConstants.OBJECT_TYPE;

public class Equals extends IntrinsicMethod {
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
        StackValue leftExpr;
        JetExpression rightExpr;
        if (element instanceof JetCallExpression) {
            leftExpr = StackValue.lazyCast(receiver, OBJECT_TYPE);
            rightExpr = arguments.get(0);
        }
        else {
            leftExpr = codegen.genLazy(arguments.get(0), OBJECT_TYPE);
            rightExpr = arguments.get(1);
        }

        genEqualsForExpressionsOnStack(JetTokens.EQEQ, leftExpr, codegen.genLazy(rightExpr, OBJECT_TYPE)).put(returnType, v);
        return returnType;
    }
}
