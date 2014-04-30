/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.resolve.calls;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.CallableDescriptor;
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor;
import org.jetbrains.jet.lang.diagnostics.Errors;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.resolve.calls.context.BasicCallResolutionContext;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCall;
import org.jetbrains.jet.lang.resolve.calls.results.OverloadResolutionResultsImpl;
import org.jetbrains.jet.lang.types.JetType;

import java.util.Map;

public class TypeParameterAsReifiedCheck implements CallResolverExtension {
    @Override
    public <F extends CallableDescriptor> void run(
            @NotNull ResolvedCall<F> resolvedCall, @NotNull BasicCallResolutionContext context
    ) {
        Map<TypeParameterDescriptor, JetType> typeArguments = resolvedCall.getTypeArguments();
        for (Map.Entry<TypeParameterDescriptor, JetType> entry : typeArguments.entrySet()) {
            TypeParameterDescriptor parameter = entry.getKey();
            JetType argument = entry.getValue();
            if (parameter.isReified() && argument.getConstructor().getDeclarationDescriptor() instanceof TypeParameterDescriptor) {
                JetExpression callee = context.call.getCalleeExpression();
                PsiElement element = callee != null ? callee : context.call.getCallElement();
                context.trace.report(Errors.TYPE_PARAMETER_AS_REIFIED.on(element, typeArguments.keySet().iterator().next()));
            }
        }
    }
}
