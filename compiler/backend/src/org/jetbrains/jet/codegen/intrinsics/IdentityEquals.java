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
import org.jetbrains.jet.lang.psi.JetCallExpression;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lexer.JetTokens;
import org.jetbrains.org.objectweb.asm.Type;

import java.util.List;

import static org.jetbrains.jet.lang.resolve.java.AsmTypes.OBJECT_TYPE;

public class IdentityEquals extends LazyIntrinsicMethod {
    @NotNull
    @Override
    public StackValue generateImpl(
            @NotNull ExpressionCodegen codegen,
            @NotNull Type returnType,
            PsiElement element,
            @NotNull List<JetExpression> arguments,
            @NotNull StackValue receiver
    ) {
        StackValue left;
        StackValue right;
        if (element instanceof JetCallExpression) {
            left = receiver;
            right = codegen.gen(arguments.get(0));
        }
        else {
            assert element instanceof JetBinaryExpression;
            JetBinaryExpression e = (JetBinaryExpression) element;
            left = codegen.gen(e.getLeft());
            right = codegen.gen(e.getRight());
        }
        return StackValue.cmp(JetTokens.EQEQEQ, OBJECT_TYPE, left, right);
    }
}
