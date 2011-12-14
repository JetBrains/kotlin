package org.jetbrains.jet.lang.resolve.calls;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.CallableDescriptor;

import java.util.Collection;

/**
 * @author abreslav
 */
public interface OverloadResolutionResults<D extends CallableDescriptor> {
    enum Code {
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

    @NotNull
    Collection<? extends ResolvedCall<? extends D>> getResults();

    @NotNull
    ResolvedCall<? extends D> getResult();

    @NotNull
    Code getResultCode();

    boolean isSuccess();

    boolean singleDescriptor();

    boolean isNothing();

    boolean isAmbiguity();
}
