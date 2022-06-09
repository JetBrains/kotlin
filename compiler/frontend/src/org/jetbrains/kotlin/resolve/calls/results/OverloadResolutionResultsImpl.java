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

package org.jetbrains.kotlin.resolve.calls.results;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.CallableDescriptor;
import org.jetbrains.kotlin.resolve.DelegatingBindingTrace;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall;

import java.util.Collection;
import java.util.Collections;

public class OverloadResolutionResultsImpl<D extends CallableDescriptor> implements OverloadResolutionResults<D> {
    public static <D extends CallableDescriptor> OverloadResolutionResultsImpl<D> nameNotFound() {
        OverloadResolutionResultsImpl<D> results = new OverloadResolutionResultsImpl<>(
                Code.NAME_NOT_FOUND, Collections.emptyList());
        results.setAllCandidates(Collections.emptyList());
        return results;
    }

    public static <D extends CallableDescriptor> OverloadResolutionResultsImpl<D> ambiguity(Collection<ResolvedCall<D>> candidates) {
        return new OverloadResolutionResultsImpl<>(Code.AMBIGUITY, candidates);
    }

    public static <D extends CallableDescriptor> OverloadResolutionResultsImpl<D> incompleteTypeInference(Collection<ResolvedCall<D>> candidates) {
        return new OverloadResolutionResultsImpl<>(Code.INCOMPLETE_TYPE_INFERENCE, candidates);
    }

    public static <D extends CallableDescriptor> OverloadResolutionResultsImpl<D> incompleteTypeInference(ResolvedCall<D> candidate) {
        return incompleteTypeInference(Collections.singleton(candidate));
    }

    private final Collection<ResolvedCall<D>> results;
    private final Code resultCode;
    private DelegatingBindingTrace trace;
    private Collection<ResolvedCall<D>> allCandidates;

    private OverloadResolutionResultsImpl(@NotNull Code resultCode, @NotNull Collection<ResolvedCall<D>> results) {
        this.results = results;
        this.resultCode = resultCode;
    }

    @Override
    @NotNull
    public Collection<ResolvedCall<D>> getResultingCalls() {
        return results;
    }

    @Override
    @NotNull
    public ResolvedCall<D> getResultingCall() {
        assert isSingleResult();
        return results.iterator().next();
    }

    @NotNull
    @Override
    public D getResultingDescriptor() {
        return getResultingCall().getResultingDescriptor();
    }

    @Override
    @NotNull
    public Code getResultCode() {
        return resultCode;
    }

    @Override
    public boolean isSuccess() {
        return resultCode.isSuccess();
    }

    @Override
    public boolean isSingleResult() {
        return results.size() == 1 && getResultCode() != Code.CANDIDATES_WITH_WRONG_RECEIVER;
    }

    @Override
    public boolean isNothing() {
        return resultCode == Code.NAME_NOT_FOUND;
    }

    @Override
    public boolean isAmbiguity() {
        return resultCode == Code.AMBIGUITY;
    }

    @Override
    public boolean isIncomplete() {
        return resultCode == Code.INCOMPLETE_TYPE_INFERENCE;
    }

    public DelegatingBindingTrace getTrace() {
        return trace;
    }

    public OverloadResolutionResultsImpl<D> setTrace(DelegatingBindingTrace trace) {
        this.trace = trace;
        return this;
    }

    public void setAllCandidates(@Nullable Collection<ResolvedCall<D>> allCandidates) {
        this.allCandidates = allCandidates;
    }

    @Nullable
    @Override
    public Collection<ResolvedCall<D>> getAllCandidates() {
        return allCandidates;
    }
}
