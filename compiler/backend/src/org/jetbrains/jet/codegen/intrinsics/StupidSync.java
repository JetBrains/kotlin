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
import org.jetbrains.annotations.Nullable;
import org.jetbrains.asm4.Type;
import org.jetbrains.asm4.commons.InstructionAdapter;
import org.jetbrains.jet.codegen.ExpressionCodegen;
import org.jetbrains.jet.codegen.StackValue;
import org.jetbrains.jet.lang.psi.JetCallExpression;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCall;

import java.util.Arrays;
import java.util.List;

import static org.jetbrains.jet.lang.resolve.java.AsmTypeConstants.FUNCTION0_TYPE;
import static org.jetbrains.jet.lang.resolve.java.AsmTypeConstants.OBJECT_TYPE;

public class StupidSync extends IntrinsicMethod {
    @NotNull
    @Override
    public Type generateImpl(
            @NotNull ExpressionCodegen codegen,
            @NotNull InstructionAdapter v,
            @NotNull Type returnType,
            @Nullable PsiElement element,
            @Nullable List<JetExpression> arguments,
            StackValue receiver
    ) {
        assert element != null : "Element should not be null";
        ResolvedCall<?> resolvedCall =
                codegen.getBindingContext().get(BindingContext.RESOLVED_CALL, ((JetCallExpression) element).getCalleeExpression());

        assert resolvedCall != null : "Resolved call for " + element.getText() + " should be not null";

        codegen.pushMethodArguments(resolvedCall, Arrays.asList(OBJECT_TYPE, FUNCTION0_TYPE), codegen.defaulCallGenerator);
        v.invokestatic("kotlin/jvm/internal/Intrinsics", "stupidSync", Type.getMethodDescriptor(OBJECT_TYPE, OBJECT_TYPE, FUNCTION0_TYPE));
        return OBJECT_TYPE;
    }
}
