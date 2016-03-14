/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.resolve.calls.tasks;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.builtins.FunctionTypesKt;
import org.jetbrains.kotlin.descriptors.CallableDescriptor;
import org.jetbrains.kotlin.psi.Call;
import org.jetbrains.kotlin.psi.KtExpression;
import org.jetbrains.kotlin.psi.KtReferenceExpression;
import org.jetbrains.kotlin.psi.KtSimpleNameExpression;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.BindingTrace;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall;
import org.jetbrains.kotlin.types.KotlinType;

import java.util.Collection;

import static org.jetbrains.kotlin.diagnostics.Errors.FUNCTION_EXPECTED;
import static org.jetbrains.kotlin.diagnostics.Errors.NO_RECEIVER_ALLOWED;
import static org.jetbrains.kotlin.resolve.BindingContext.CALL;
import static org.jetbrains.kotlin.resolve.BindingContext.RESOLVED_CALL;

public class TracingStrategyForInvoke extends AbstractTracingStrategy {
    private final KotlinType calleeType;

    public TracingStrategyForInvoke(
            @NotNull KtExpression reference,
            @NotNull Call call,
            @NotNull KotlinType calleeType
    ) {
        super(reference, call);
        this.calleeType = calleeType;
    }

    @Override
    public void bindCall(@NotNull BindingTrace trace, @NotNull Call call) {
        // If reference is a simple name, it's 'variable as function call' case ('foo(a, b)' where 'foo' is a variable).
        // The outer call is bound ('foo(a, b)'), while 'invoke' call for this case is 'foo.invoke(a, b)' and shouldn't be bound.
        if (reference instanceof KtSimpleNameExpression) return;
        trace.record(CALL, reference, call);
    }

    @Override
    public <D extends CallableDescriptor> void bindReference(
            @NotNull BindingTrace trace, @NotNull ResolvedCall<D> resolvedCall
    ) {
        PsiElement callElement = call.getCallElement();
        if (callElement instanceof KtReferenceExpression) {
            trace.record(BindingContext.REFERENCE_TARGET, (KtReferenceExpression) callElement, resolvedCall.getCandidateDescriptor());
        }
    }

    @Override
    public <D extends CallableDescriptor> void bindResolvedCall(
            @NotNull BindingTrace trace, @NotNull ResolvedCall<D> resolvedCall
    ) {
        if (reference instanceof KtSimpleNameExpression) return;
        trace.record(RESOLVED_CALL, call, resolvedCall);
    }

    @Override
    public void unresolvedReference(@NotNull BindingTrace trace) {
        functionExpectedOrNoReceiverAllowed(trace);
    }

    @Override
    public <D extends CallableDescriptor> void unresolvedReferenceWrongReceiver(
            @NotNull BindingTrace trace, @NotNull Collection<? extends ResolvedCall<D>> candidates
    ) {
        functionExpectedOrNoReceiverAllowed(trace);
    }

    private void functionExpectedOrNoReceiverAllowed(BindingTrace trace) {
        if (FunctionTypesKt.isNonExtensionFunctionType(calleeType)) {
            trace.report(NO_RECEIVER_ALLOWED.on(reference));
        }
        else {
            trace.report(FUNCTION_EXPECTED.on(reference, reference, calleeType));
        }
    }
}
