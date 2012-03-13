/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.codegen.ExpressionCodegen;
import org.jetbrains.jet.codegen.JetTypeMapper;
import org.jetbrains.jet.codegen.StackValue;
import org.jetbrains.jet.lang.descriptors.CallableDescriptor;
import org.jetbrains.jet.lang.psi.JetCallExpression;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.calls.ResolvedCall;
import org.jetbrains.jet.lang.types.TypeProjection;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.InstructionAdapter;

import java.util.List;

/**
 * @author alex.tkachman
 */
public class JavaClassFunction implements IntrinsicMethod {
    @Override
    public StackValue generate(ExpressionCodegen codegen, InstructionAdapter v, Type expectedType, @Nullable PsiElement element, @Nullable List<JetExpression> arguments, StackValue receiver) {
        JetCallExpression call = (JetCallExpression) element;
        ResolvedCall<? extends CallableDescriptor> resolvedCall = codegen.getBindingContext().get(BindingContext.RESOLVED_CALL, call.getCalleeExpression());
        CallableDescriptor resultingDescriptor = resolvedCall.getResultingDescriptor();
        Type type = codegen.getTypeMapper().mapType(resultingDescriptor.getReturnType().getArguments().get(0).getType());
        v.aconst(type);
        return StackValue.onStack(JetTypeMapper.JL_CLASS_TYPE);
    }
}
