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
import org.jetbrains.jet.lang.descriptors.CallableDescriptor;
import org.jetbrains.jet.lang.psi.JetCallExpression;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCall;
import org.jetbrains.jet.lang.types.JetType;

import java.util.List;

import static org.jetbrains.jet.codegen.AsmUtil.boxType;
import static org.jetbrains.jet.codegen.AsmUtil.isPrimitive;
import static org.jetbrains.jet.lang.resolve.java.AsmTypeConstants.getType;

public class JavaClassFunction extends IntrinsicMethod {
    @NotNull
    @Override
    public Type generateImpl(
            @NotNull ExpressionCodegen codegen,
            @NotNull InstructionAdapter v,
            @NotNull Type expectedType,
            @Nullable PsiElement element,
            @Nullable List<JetExpression> arguments,
            StackValue receiver
    ) {
        JetCallExpression call = (JetCallExpression) element;
        ResolvedCall<? extends CallableDescriptor> resolvedCall =
                codegen.getBindingContext().get(BindingContext.RESOLVED_CALL, call.getCalleeExpression());
        assert resolvedCall != null;
        JetType returnType = resolvedCall.getResultingDescriptor().getReturnType();
        assert returnType != null;
        Type type = codegen.getState().getTypeMapper().mapType(returnType.getArguments().get(0).getType());
        if (isPrimitive(type)) {
            v.getstatic(boxType(type).getInternalName(), "TYPE", "Ljava/lang/Class;");
        }
        else {
            v.aconst(type);
        }

        return getType(Class.class);
    }
}
