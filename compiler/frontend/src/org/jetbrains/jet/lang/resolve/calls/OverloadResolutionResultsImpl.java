package org.jetbrains.jet.lang.resolve.calls;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.CallableDescriptor;

import java.util.Collection;
import java.util.Collections;

/**
* @author abreslav
*/
public class OverloadResolutionResultsImpl<D extends CallableDescriptor> implements OverloadResolutionResults<D> {

    public static <D extends CallableDescriptor> OverloadResolutionResultsImpl<D> success(@NotNull ResolvedCallImpl<D> descriptor) {
        return new OverloadResolutionResultsImpl<D>(Code.SUCCESS, Collections.singleton(descriptor));
    }

    public static <D extends CallableDescriptor> OverloadResolutionResultsImpl<D> nameNotFound() {
        return new OverloadResolutionResultsImpl<D>(Code.NAME_NOT_FOUND, Collections.<ResolvedCallImpl<D>>emptyList());
    }

    public static <D extends CallableDescriptor> OverloadResolutionResultsImpl<D> singleFailedCandidate(ResolvedCallImpl<D> candidate) {
        return new OverloadResolutionResultsImpl<D>(Code.SINGLE_CANDIDATE_ARGUMENT_MISMATCH, Collections.singleton(candidate));
    }
    public static <D extends CallableDescriptor> OverloadResolutionResultsImpl<D> manyFailedCandidates(Collection<ResolvedCallImpl<D>> failedCandidates) {
        return new OverloadResolutionResultsImpl<D>(Code.MANY_FAILED_CANDIDATES, failedCandidates);
    }

    public static <D extends CallableDescriptor> OverloadResolutionResultsImpl<D> ambiguity(Collection<ResolvedCallImpl<D>> descriptors) {
        return new OverloadResolutionResultsImpl<D>(Code.AMBIGUITY, descriptors);
    }

    private final Collection<ResolvedCallImpl<D>> results;
    private final Code resultCode;

    private OverloadResolutionResultsImpl(@NotNull Code resultCode, @NotNull Collection<ResolvedCallImpl<D>> results) {
        this.results = results;
        this.resultCode = resultCode;
    }

    @Override
    @NotNull
    public Collection<ResolvedCallImpl<D>> getResults() {
        return results;
    }

    @Override
    @NotNull
    public ResolvedCallImpl<D> getResult() {
        assert singleDescriptor();
        return results.iterator().next();
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
    public boolean singleDescriptor() {
        return isSuccess() || resultCode == Code.SINGLE_CANDIDATE_ARGUMENT_MISMATCH;
    }

    @Override
    public boolean isNothing() {
        return resultCode == Code.NAME_NOT_FOUND;
    }

    @Override
    public boolean isAmbiguity() {
        return resultCode == Code.AMBIGUITY;
    }
//
//    public OverloadResolutionResultsImpl<D> newContents(@NotNull Collection<D> functionDescriptors) {
//        return new OverloadResolutionResultsImpl<D>(resultCode, functionDescriptors);
//    }
}
