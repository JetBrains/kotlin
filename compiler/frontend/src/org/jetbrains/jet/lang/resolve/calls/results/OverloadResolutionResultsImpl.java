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
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.CallableDescriptor;
import org.jetbrains.jet.lang.resolve.DelegatingBindingTrace;
import org.jetbrains.jet.lang.resolve.calls.model.MutableResolvedCall;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCall;

import java.util.Collection;
import java.util.Collections;

public class OverloadResolutionResultsImpl<D extends CallableDescriptor> implements OverloadResolutionResults<D> {

    public static <D extends CallableDescriptor> OverloadResolutionResultsImpl<D> success(@NotNull MutableResolvedCall<D> candidate) {
        return new OverloadResolutionResultsImpl<D>(Code.SUCCESS, Collections.singleton(candidate));
    }

    public static <D extends CallableDescriptor> OverloadResolutionResultsImpl<D> nameNotFound() {
        OverloadResolutionResultsImpl<D> results = new OverloadResolutionResultsImpl<D>(
                Code.NAME_NOT_FOUND, Collections.<MutableResolvedCall<D>>emptyList());
        results.setAllCandidates(Collections.<ResolvedCall<D>>emptyList());
        return results;
    }

    public static <D extends CallableDescriptor> OverloadResolutionResultsImpl<D> singleFailedCandidate(MutableResolvedCall<D> candidate) {
        return new OverloadResolutionResultsImpl<D>(Code.SINGLE_CANDIDATE_ARGUMENT_MISMATCH, Collections.singleton(candidate));
    }
    public static <D extends CallableDescriptor> OverloadResolutionResultsImpl<D> manyFailedCandidates(Collection<MutableResolvedCall<D>> failedCandidates) {
        return new OverloadResolutionResultsImpl<D>(Code.MANY_FAILED_CANDIDATES, failedCandidates);
    }

    public static <D extends CallableDescriptor> OverloadResolutionResultsImpl<D> candidatesWithWrongReceiver(Collection<MutableResolvedCall<D>> failedCandidates) {
        return new OverloadResolutionResultsImpl<D>(Code.CANDIDATES_WITH_WRONG_RECEIVER, failedCandidates);
    }

    public static <D extends CallableDescriptor> OverloadResolutionResultsImpl<D> ambiguity(Collection<MutableResolvedCall<D>> candidates) {
        return new OverloadResolutionResultsImpl<D>(Code.AMBIGUITY, candidates);
    }

    public static <D extends CallableDescriptor> OverloadResolutionResultsImpl<D> incompleteTypeInference(Collection<MutableResolvedCall<D>> candidates) {
        return new OverloadResolutionResultsImpl<D>(Code.INCOMPLETE_TYPE_INFERENCE, candidates);
    }

    public static <D extends CallableDescriptor> OverloadResolutionResultsImpl<D> incompleteTypeInference(MutableResolvedCall<D> candidate) {
        return incompleteTypeInference(Collections.singleton(candidate));
    }

    private final Collection<MutableResolvedCall<D>> results;
    private final Code resultCode;
    private DelegatingBindingTrace trace;
    private Collection<ResolvedCall<D>> allCandidates;

    private OverloadResolutionResultsImpl(@NotNull Code resultCode, @NotNull Collection<MutableResolvedCall<D>> results) {
        this.results = results;
        this.resultCode = resultCode;
    }

    @Override
    @NotNull
    public Collection<MutableResolvedCall<D>> getResultingCalls() {
        return results;
    }

    @Override
    @NotNull
    public MutableResolvedCall<D> getResultingCall() {
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

    @NotNull
    public OverloadResolutionResultsImpl<D> changeStatusToSuccess() {
        if (getResultCode() == Code.SUCCESS) return this;

        assert isSingleResult() && getResultCode() == Code.INCOMPLETE_TYPE_INFERENCE :
                "Only incomplete type inference status with one candidate can be changed to success: " +
                getResultCode() + "\n" + getResultingCalls();
        OverloadResolutionResultsImpl<D> newResults = new OverloadResolutionResultsImpl<D>(Code.SUCCESS, getResultingCalls());
        newResults.setAllCandidates(getAllCandidates());
        return newResults;
    }
}
