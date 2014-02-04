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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.CallableDescriptor;
import org.jetbrains.jet.lang.psi.Call;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCallWithTrace;

import java.util.Collection;

import static org.jetbrains.jet.lang.resolve.BindingContext.CALL;
import static org.jetbrains.jet.lang.resolve.BindingContext.RESOLVED_CALL;

public class TracingStrategyForInvoke extends AbstractTracingStrategy {
    public TracingStrategyForInvoke(
            @NotNull JetExpression reference,
            @NotNull Call call
    ) {
        super(reference, call);
    }

    @Override
    public <D extends CallableDescriptor> void bindReference(
            @NotNull BindingTrace trace, @NotNull ResolvedCallWithTrace<D> resolvedCall
    ) {
    }

    @Override
    public <D extends CallableDescriptor> void bindResolvedCall(
            @NotNull BindingTrace trace, @NotNull ResolvedCallWithTrace<D> resolvedCall
    ) {
        trace.record(RESOLVED_CALL, reference, resolvedCall);
        trace.record(CALL, reference, call);
    }

    @Override
    public void unresolvedReference(@NotNull BindingTrace trace) {
    }

    @Override
    public <D extends CallableDescriptor> void unresolvedReferenceWrongReceiver(
            @NotNull BindingTrace trace, @NotNull Collection<ResolvedCallWithTrace<D>> candidates
    ) {
    }
}
