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

package org.jetbrains.jet.lang.resolve.calls;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.CallableDescriptor;
import org.jetbrains.jet.lang.psi.JetCallExpression;
import org.jetbrains.jet.lang.resolve.calls.context.BasicCallResolutionContext;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCall;
import org.jetbrains.jet.lang.resolve.calls.results.OverloadResolutionResultsImpl;

public abstract class RecursiveCallResolverExtension implements CallResolverExtension {
    @Override
    public <F extends CallableDescriptor> void run(
            @NotNull ResolvedCall<F> resolvedCall, @NotNull BasicCallResolutionContext context
    ) {
        if (isRecursion(resolvedCall, context) && context.call.getCallElement() instanceof JetCallExpression) {
            runImpl((JetCallExpression) context.call.getCallElement(), resolvedCall, context);
        }
    }

    protected abstract <F extends CallableDescriptor> void runImpl(@NotNull JetCallExpression callExpression,
            @NotNull ResolvedCall<F> resolvedCall, @NotNull BasicCallResolutionContext context);

    private static <F extends CallableDescriptor> boolean isRecursion(
            @NotNull ResolvedCall<F> resolvedCall,
            @NotNull BasicCallResolutionContext context
    ) {
        return resolvedCall.getResultingDescriptor().getOriginal().equals(context.scope.getContainingDeclaration());
    }
}
