package org.jetbrains.jet.lang.resolve.calls;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

/**
* @author abreslav
*/
public class OverloadResolutionResult<D> {
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

    public static <D> OverloadResolutionResult<D> success(@NotNull D descriptor) {
        return new OverloadResolutionResult<D>(Code.SUCCESS, Collections.singleton(descriptor));
    }

    public static <D> OverloadResolutionResult<D> nameNotFound() {
        return new OverloadResolutionResult<D>(Code.NAME_NOT_FOUND, Collections.<D>emptyList());
    }

    public static <D> OverloadResolutionResult<D> singleFailedCandidate(D candidate) {
        return new OverloadResolutionResult<D>(Code.SINGLE_CANDIDATE_ARGUMENT_MISMATCH, Collections.singleton(candidate));
    }
    public static <D> OverloadResolutionResult<D> manyFailedCandidates(Set<D> failedCandidates) {
        return new OverloadResolutionResult<D>(Code.MANY_FAILED_CANDIDATES, failedCandidates);
    }

    public static <D> OverloadResolutionResult<D> ambiguity(Collection<D> descriptors) {
        return new OverloadResolutionResult<D>(Code.AMBIGUITY, descriptors);
    }

    private final Collection<D> descriptors;

    private final Code resultCode;

    public OverloadResolutionResult(@NotNull Code resultCode, @NotNull Collection<D> descriptors) {
        this.descriptors = descriptors;
        this.resultCode = resultCode;

    }

    @NotNull
    public Collection<D> getDescriptors() {
        return descriptors;
    }

    @NotNull
    public D getDescriptor() {
        assert singleDescriptor();
        return descriptors.iterator().next();
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

    public OverloadResolutionResult<D> newContents(@NotNull Collection<D> functionDescriptors) {
        return new OverloadResolutionResult<D>(resultCode, functionDescriptors);
    }
}
