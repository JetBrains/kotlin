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
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.codegen.ExpressionCodegen;
import org.jetbrains.jet.codegen.GenerationState;
import org.jetbrains.jet.codegen.StackValue;
import org.jetbrains.jet.lang.descriptors.CallableDescriptor;
import org.jetbrains.jet.lang.descriptors.ValueParameterDescriptor;
import org.jetbrains.jet.lang.psi.JetCallExpression;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.calls.ResolvedCall;
import org.jetbrains.jet.lang.resolve.calls.ResolvedValueArgument;
import org.jetbrains.jet.lang.resolve.calls.VarargValueArgument;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.InstructionAdapter;

import java.util.List;
import java.util.Map;

/**
 * @author alex.tkachman
 */
public class JavaClassArray implements IntrinsicMethod {
    @Override
    public StackValue generate(ExpressionCodegen codegen,
                               InstructionAdapter v,
                               Type expectedType,
                               @Nullable PsiElement element,
                               @Nullable List<JetExpression> arguments,
                               StackValue receiver,
                               @NotNull GenerationState state) {
        ResolvedCall<? extends CallableDescriptor> call =
            codegen.getBindingContext().get(BindingContext.RESOLVED_CALL, ((JetCallExpression)element).getCalleeExpression());
        Map.Entry<ValueParameterDescriptor,ResolvedValueArgument> next = call.getValueArguments().entrySet().iterator().next();
        codegen.genVarargs(next.getKey(), (VarargValueArgument)next.getValue());
        return StackValue.onStack(expectedType);
    }
}
