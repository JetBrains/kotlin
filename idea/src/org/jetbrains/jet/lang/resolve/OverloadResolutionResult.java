package org.jetbrains.jet.lang.resolve;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;

import java.util.Collection;
import java.util.Collections;

/**
* @author abreslav
*/
public class OverloadResolutionResult {
    public enum Code {
        SUCCESS(true),
        NAME_NOT_FOUND(false),
        SINGLE_FUNCTION_ARGUMENT_MISMATCH(false),
        AMBIGUITY(false);

        private final boolean success;

        Code(boolean success) {
            this.success = success;
        }

        boolean isSuccess() {
            return success;
        }

    }

    public static OverloadResolutionResult success(@NotNull FunctionDescriptor functionDescriptor) {
        return new OverloadResolutionResult(Code.SUCCESS, Collections.singleton(functionDescriptor));
    }

    public static OverloadResolutionResult nameNotFound() {
        return new OverloadResolutionResult(Code.NAME_NOT_FOUND, Collections.<FunctionDescriptor>emptyList());
    }
    public static OverloadResolutionResult singleFunctionArgumentMismatch(FunctionDescriptor functionDescriptor) {
        return new OverloadResolutionResult(Code.SINGLE_FUNCTION_ARGUMENT_MISMATCH, Collections.singleton(functionDescriptor));
    }

    public static OverloadResolutionResult ambiguity(Collection<FunctionDescriptor> functionDescriptors) {
        return new OverloadResolutionResult(Code.AMBIGUITY, functionDescriptors);
    }

    private final Collection<FunctionDescriptor> functionDescriptors;

    private final Code resultCode;

    public OverloadResolutionResult(@NotNull Code resultCode, @NotNull Collection<FunctionDescriptor> functionDescriptors) {
        this.functionDescriptors = functionDescriptors;
        this.resultCode = resultCode;

    }

    @NotNull
    public Collection<FunctionDescriptor> getFunctionDescriptors() {
        return functionDescriptors;
    }

    @NotNull
    public FunctionDescriptor getFunctionDescriptor() {
        assert singleFunction();
        return functionDescriptors.iterator().next();
    }

    @NotNull
    public Code getResultCode() {
        return resultCode;
    }

    public boolean isSuccess() {
        return resultCode.isSuccess();
    }

    public boolean singleFunction() {
        return isSuccess() || resultCode == Code.SINGLE_FUNCTION_ARGUMENT_MISMATCH;
    }

    public boolean isNothing() {
        return resultCode == Code.NAME_NOT_FOUND;
    }

    public boolean isAmbiguity() {
        return resultCode == Code.AMBIGUITY;
    }

    public OverloadResolutionResult newContents(@NotNull Collection<FunctionDescriptor> functionDescriptors) {
        return new OverloadResolutionResult(resultCode, functionDescriptors);
    }
}
