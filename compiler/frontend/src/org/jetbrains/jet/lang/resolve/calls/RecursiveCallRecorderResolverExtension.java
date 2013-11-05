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
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.psi.JetCallExpression;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.calls.context.BasicCallResolutionContext;
import org.jetbrains.jet.lang.resolve.calls.results.OverloadResolutionResultsImpl;

import java.util.ArrayList;
import java.util.List;

public class RecursiveCallRecorderResolverExtension extends RecursiveCallResolverExtension {

    public static final RecursiveCallRecorderResolverExtension INSTANCE = new RecursiveCallRecorderResolverExtension();

    @Override
    protected <F extends CallableDescriptor> void runImpl(
            @NotNull JetCallExpression callExpression,
            @NotNull OverloadResolutionResultsImpl<F> results,
            @NotNull BasicCallResolutionContext context
    ) {
        DeclarationDescriptor declarationDescriptor = context.scope.getContainingDeclaration();

        List<JetCallExpression> expressions =
                context.trace.get(BindingContext.FUNCTION_RECURSIVE_CALL_EXPRESSIONS, declarationDescriptor);

        if (expressions == null) {
            expressions = new ArrayList<JetCallExpression>(2);
            context.trace.record(BindingContext.FUNCTION_RECURSIVE_CALL_EXPRESSIONS, declarationDescriptor, expressions);
        }

        expressions.add(callExpression);
    }
}
