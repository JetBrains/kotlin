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

package org.jetbrains.jet.lang.resolve.calls.tasks;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.CallableDescriptor;
import org.jetbrains.jet.lang.psi.Call;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.psi.JetSimpleNameExpression;
import org.jetbrains.jet.lang.psi.JetReferenceExpression;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCall;
import org.jetbrains.jet.lang.types.JetType;

import java.util.Collection;

import static org.jetbrains.jet.lang.diagnostics.Errors.FUNCTION_EXPECTED;
import static org.jetbrains.jet.lang.resolve.BindingContext.CALL;
import static org.jetbrains.jet.lang.resolve.BindingContext.RESOLVED_CALL;

public class TracingStrategyForInvoke extends AbstractTracingStrategy {
    private final JetType calleeType;

    public TracingStrategyForInvoke(
            @NotNull JetExpression reference,
            @NotNull Call call,
            @NotNull JetType calleeType
    ) {
        super(reference, call);
        this.calleeType = calleeType;
    }

    @Override
    public void bindCall(@NotNull BindingTrace trace, @NotNull Call call) {
        // If reference is a simple name, it's 'variable as function call' case ('foo(a, b)' where 'foo' is a variable).
        // The outer call is bound ('foo(a, b)'), while 'invoke' call for this case is 'foo.invoke(a, b)' and shouldn't be bound.
        if (reference instanceof JetSimpleNameExpression) return;
        trace.record(CALL, reference, call);
    }

    @Override
    public <D extends CallableDescriptor> void bindReference(
            @NotNull BindingTrace trace, @NotNull ResolvedCall<D> resolvedCall
    ) {
        PsiElement callElement = call.getCallElement();
        if (callElement instanceof JetReferenceExpression) {
            trace.record(BindingContext.REFERENCE_TARGET, (JetReferenceExpression) callElement, resolvedCall.getCandidateDescriptor());
        }
    }

    @Override
    public <D extends CallableDescriptor> void bindResolvedCall(
            @NotNull BindingTrace trace, @NotNull ResolvedCall<D> resolvedCall
    ) {
        if (reference instanceof JetSimpleNameExpression) return;
        trace.record(RESOLVED_CALL, call, resolvedCall);
    }

    @Override
    public void unresolvedReference(@NotNull BindingTrace trace) {
        trace.report(FUNCTION_EXPECTED.on(reference, reference, calleeType));
    }

    @Override
    public <D extends CallableDescriptor> void unresolvedReferenceWrongReceiver(
            @NotNull BindingTrace trace, @NotNull Collection<? extends ResolvedCall<D>> candidates
    ) {
        trace.report(FUNCTION_EXPECTED.on(reference, reference, calleeType));
    }
}
