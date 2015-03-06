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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.descriptors.CallableDescriptor;
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor;
import org.jetbrains.kotlin.psi.Call;
import org.jetbrains.kotlin.psi.JetReferenceExpression;
import org.jetbrains.kotlin.resolve.BindingTrace;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall;
import org.jetbrains.kotlin.resolve.calls.model.VariableAsFunctionResolvedCall;
import org.jetbrains.kotlin.resolve.calls.util.FakeCallableDescriptorForObject;
import org.jetbrains.kotlin.types.ErrorUtils;

import java.util.Collection;

import static org.jetbrains.kotlin.diagnostics.Errors.UNRESOLVED_REFERENCE;
import static org.jetbrains.kotlin.diagnostics.Errors.UNRESOLVED_REFERENCE_WRONG_RECEIVER;
import static org.jetbrains.kotlin.resolve.BindingContext.*;

public class TracingStrategyImpl extends AbstractTracingStrategy {
    private final JetReferenceExpression reference;

    private TracingStrategyImpl(@NotNull JetReferenceExpression reference, @NotNull Call call) {
        super(reference, call);
        this.reference = reference;
    }

    @NotNull
    public static TracingStrategy create(@NotNull JetReferenceExpression reference, @NotNull Call call) {
        return new TracingStrategyImpl(reference, call);
    }

    @Override
    public void bindCall(@NotNull BindingTrace trace, @NotNull Call call) {
        trace.record(CALL, call.getCalleeExpression(), call);
    }

    @Override
    public <D extends CallableDescriptor> void bindReference(@NotNull BindingTrace trace, @NotNull ResolvedCall<D> resolvedCall) {
        DeclarationDescriptor descriptor = resolvedCall.getCandidateDescriptor();
        if (resolvedCall instanceof VariableAsFunctionResolvedCall) {
            descriptor = ((VariableAsFunctionResolvedCall) resolvedCall).getVariableCall().getCandidateDescriptor();
        }
        if (descriptor instanceof FakeCallableDescriptorForObject) {
            FakeCallableDescriptorForObject fakeCallableDescriptorForObject = (FakeCallableDescriptorForObject) descriptor;
            descriptor = fakeCallableDescriptorForObject.getReferencedDescriptor();
            if (fakeCallableDescriptorForObject.getClassDescriptor().getDefaultObjectDescriptor() != null) {
                trace.record(SHORT_REFERENCE_TO_DEFAULT_OBJECT, reference, fakeCallableDescriptorForObject.getClassDescriptor());
            }
        }
        DeclarationDescriptor storedReference = trace.get(REFERENCE_TARGET, reference);
        if (storedReference == null || !ErrorUtils.isError(descriptor)) {
            trace.record(REFERENCE_TARGET, reference, descriptor);
        }
    }

    @Override
    public <D extends CallableDescriptor> void bindResolvedCall(@NotNull BindingTrace trace, @NotNull ResolvedCall<D> resolvedCall) {
        trace.record(RESOLVED_CALL, call, resolvedCall);
    }

    @Override
    public void unresolvedReference(@NotNull BindingTrace trace) {
        trace.report(UNRESOLVED_REFERENCE.on(reference, reference));
    }

    @Override
    public <D extends CallableDescriptor> void unresolvedReferenceWrongReceiver(@NotNull BindingTrace trace, @NotNull Collection<? extends ResolvedCall<D>> candidates) {
        trace.report(UNRESOLVED_REFERENCE_WRONG_RECEIVER.on(reference, candidates));
    }
}
