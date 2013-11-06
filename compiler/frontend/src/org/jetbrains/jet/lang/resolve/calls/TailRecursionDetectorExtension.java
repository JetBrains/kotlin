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
import org.jetbrains.jet.lang.diagnostics.Errors;
import org.jetbrains.jet.lang.psi.JetCallExpression;
import org.jetbrains.jet.lang.psi.JetPsiUtil;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.calls.context.BasicCallResolutionContext;
import org.jetbrains.jet.lang.resolve.calls.results.OverloadResolutionResultsImpl;

public class TailRecursionDetectorExtension extends RecursiveCallResolverExtension {

    public static final TailRecursionDetectorExtension INSTANCE = new TailRecursionDetectorExtension();

    @Override
    protected <F extends CallableDescriptor> void runImpl(
            @NotNull JetCallExpression callExpression,
            @NotNull OverloadResolutionResultsImpl<F> results,
            @NotNull BasicCallResolutionContext context
    ) {
        BindingTrace trace = context.trace;

        TailRecursionKind recursionKind =
                JetPsiUtil.visitUpwardToRoot(callExpression, new TailRecursionDetectorVisitor(), TailRecursionKind.MIGHT_BE);

        trace.record(BindingContext.TAIL_RECURSION_CALL,
                     callExpression,
                     recursionKind);

        switch (recursionKind) {
            case NON_TAIL:
                trace.report(Errors.NON_TAIL_RECURSIVE_CALL.on(callExpression));
                break;
            case IN_FINALLY:
                trace.report(Errors.TAIL_RECURSION_IN_TRY_IS_NOT_SUPPORTED.on(callExpression));
                break;
            default:
                break;
        }
    }
}
