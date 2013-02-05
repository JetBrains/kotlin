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

package org.jetbrains.jet.lang.resolve.calls.results;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.CallableDescriptor;
import org.jetbrains.jet.lang.resolve.DelegatingBindingTrace;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCallWithTrace;

import java.util.Collection;
import java.util.Collections;

public class OverloadResolutionResultsImpl<D extends CallableDescriptor> implements OverloadResolutionResults<D> {

    public static <D extends CallableDescriptor> OverloadResolutionResultsImpl<D> success(@NotNull ResolvedCallWithTrace<D> candidate) {
        return new OverloadResolutionResultsImpl<D>(Code.SUCCESS, Collections.singleton(candidate));
    }

    public static <D extends CallableDescriptor> OverloadResolutionResultsImpl<D> nameNotFound() {
        return new OverloadResolutionResultsImpl<D>(Code.NAME_NOT_FOUND, Collections.<ResolvedCallWithTrace<D>>emptyList());
    }

    public static <D extends CallableDescriptor> OverloadResolutionResultsImpl<D> singleFailedCandidate(ResolvedCallWithTrace<D> candidate) {
        return new OverloadResolutionResultsImpl<D>(Code.SINGLE_CANDIDATE_ARGUMENT_MISMATCH, Collections.singleton(candidate));
    }
    public static <D extends CallableDescriptor> OverloadResolutionResultsImpl<D> manyFailedCandidates(Collection<ResolvedCallWithTrace<D>> failedCandidates) {
        return new OverloadResolutionResultsImpl<D>(Code.MANY_FAILED_CANDIDATES, failedCandidates);
    }

    public static <D extends CallableDescriptor> OverloadResolutionResultsImpl<D> ambiguity(Collection<ResolvedCallWithTrace<D>> candidates) {
        return new OverloadResolutionResultsImpl<D>(Code.AMBIGUITY, candidates);
    }

    public static <D extends CallableDescriptor> OverloadResolutionResultsImpl<D> incompleteTypeInference(Collection<ResolvedCallWithTrace<D>> candidates) {
        return new OverloadResolutionResultsImpl<D>(Code.INCOMPLETE_TYPE_INFERENCE, candidates);
    }

    public static <D extends CallableDescriptor> OverloadResolutionResultsImpl<D> incompleteTypeInference(ResolvedCallWithTrace<D> candidate) {
        return incompleteTypeInference(Collections.singleton(candidate));
    }

    private final Collection<ResolvedCallWithTrace<D>> results;
    private final Code resultCode;
    private DelegatingBindingTrace trace;

    private OverloadResolutionResultsImpl(@NotNull Code resultCode, @NotNull Collection<ResolvedCallWithTrace<D>> results) {
        this.results = results;
        this.resultCode = resultCode;
    }

    @Override
    @NotNull
    public Collection<ResolvedCallWithTrace<D>> getResultingCalls() {
        return results;
    }

    @Override
    @NotNull
    public ResolvedCallWithTrace<D> getResultingCall() {
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
        return results.size() == 1;
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

    //
//    public OverloadResolutionResultsImpl<D> newContents(@NotNull Collection<D> functionDescriptors) {
//        return new OverloadResolutionResultsImpl<D>(resultCode, functionDescriptors);
//    }

    public DelegatingBindingTrace getTrace() {
        return trace;
    }

    public OverloadResolutionResultsImpl<D> setTrace(DelegatingBindingTrace trace) {
        this.trace = trace;
        return this;
    }
}
