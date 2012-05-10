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
import org.jetbrains.jet.codegen.*;
import org.jetbrains.jet.lang.descriptors.SimpleFunctionDescriptor;
import org.jetbrains.jet.lang.psi.JetCallExpression;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.InstructionAdapter;

import java.util.List;

/**
 * @author yole
 * @author alex.tkachman
 */
public class PsiMethodCall implements IntrinsicMethod {
    private final SimpleFunctionDescriptor myMethod;

    public PsiMethodCall(SimpleFunctionDescriptor method) {
        myMethod = method;
    }

    @Override
    public StackValue generate(ExpressionCodegen codegen, InstructionAdapter v, Type expectedType, PsiElement element,
            List<JetExpression> arguments, StackValue receiver, @NotNull GenerationState state) {
        final CallableMethod callableMethod = state.getInjector().getJetTypeMapper().mapToCallableMethod(myMethod, false, OwnerKind.IMPLEMENTATION);
        codegen.invokeMethodWithArguments(callableMethod, (JetCallExpression) element, receiver);
        return StackValue.onStack(callableMethod.getSignature().getAsmMethod().getReturnType());
    }
}
