package org.jetbrains.jet.lang.resolve.calls;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.CallableDescriptor;

import java.util.Collection;
import java.util.Collections;

/**
* @author abreslav
*/
public class OverloadResolutionResults<D extends CallableDescriptor> {
    public enum Code {
        SUCCESS(true),
        NAME_NOT_FOUND(false),
        SINGLE_CANDIDATE_ARGUMENT_MISMATCH(false),
        AMBIGUITY(false),
        MANY_FAILED_CANDIDATES(false);

        private final boolean success;

        Code(boolean success) {
            this.success = success;
        }

        boolean isSuccess() {
            return success;
        }

    }

    public static <D extends CallableDescriptor> OverloadResolutionResults<D> success(@NotNull ResolvedCall<D> descriptor) {
        return new OverloadResolutionResults<D>(Code.SUCCESS, Collections.singleton(descriptor));
    }

    public static <D extends CallableDescriptor> OverloadResolutionResults<D> nameNotFound() {
        return new OverloadResolutionResults<D>(Code.NAME_NOT_FOUND, Collections.<ResolvedCall<D>>emptyList());
    }

    public static <D extends CallableDescriptor> OverloadResolutionResults<D> singleFailedCandidate(ResolvedCall<D> candidate) {
        return new OverloadResolutionResults<D>(Code.SINGLE_CANDIDATE_ARGUMENT_MISMATCH, Collections.singleton(candidate));
    }
    public static <D extends CallableDescriptor> OverloadResolutionResults<D> manyFailedCandidates(Collection<ResolvedCall<D>> failedCandidates) {
        return new OverloadResolutionResults<D>(Code.MANY_FAILED_CANDIDATES, failedCandidates);
    }

    public static <D extends CallableDescriptor> OverloadResolutionResults<D> ambiguity(Collection<ResolvedCall<D>> descriptors) {
        return new OverloadResolutionResults<D>(Code.AMBIGUITY, descriptors);
    }

    private final Collection<ResolvedCall<D>> results;
    private final Code resultCode;

    private OverloadResolutionResults(@NotNull Code resultCode, @NotNull Collection<ResolvedCall<D>> results) {
        this.results = results;
        this.resultCode = resultCode;
    }

    @NotNull
    public Collection<ResolvedCall<D>> getResults() {
        return results;
    }

    @NotNull
    public ResolvedCall<D> getResult() {
        assert singleDescriptor();
        return results.iterator().next();
    }

    @NotNull
    public Code getResultCode() {
        return resultCode;
    }

    public boolean isSuccess() {
        return resultCode.isSuccess();
    }

    public boolean singleDescriptor() {
        return isSuccess() || resultCode == Code.SINGLE_CANDIDATE_ARGUMENT_MISMATCH;
    }

    public boolean isNothing() {
        return resultCode == Code.NAME_NOT_FOUND;
    }

    public boolean isAmbiguity() {
        return resultCode == Code.AMBIGUITY;
    }
//
//    public OverloadResolutionResults<D> newContents(@NotNull Collection<D> functionDescriptors) {
//        return new OverloadResolutionResults<D>(resultCode, functionDescriptors);
//    }
}
