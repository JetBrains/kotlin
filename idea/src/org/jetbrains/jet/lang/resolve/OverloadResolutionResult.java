package org.jetbrains.jet.lang.resolve;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.types.FunctionDescriptor;

import java.util.Collection;

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
        return new OverloadResolutionResult(Code.SUCCESS, functionDescriptor);
    }

    public static OverloadResolutionResult nameNotFound() {
        return new OverloadResolutionResult(Code.NAME_NOT_FOUND, null);
    }

    public static OverloadResolutionResult singleFunctionArgumentMismatch(FunctionDescriptor functionDescriptor) {
        return new OverloadResolutionResult(Code.SINGLE_FUNCTION_ARGUMENT_MISMATCH, functionDescriptor);
    }

    public static OverloadResolutionResult ambiguity(Collection<FunctionDescriptor> functionDescriptors) {
        return new OverloadResolutionResult(Code.AMBIGUITY, null); // TODO
    }

    private final FunctionDescriptor functionDescriptor;
    private final Code resultCode;

    public OverloadResolutionResult(@NotNull Code resultCode, FunctionDescriptor functionDescriptor) {
        this.functionDescriptor = functionDescriptor;
        this.resultCode = resultCode;

    }

    @NotNull // This is done on purpose, despite the fact that errors may not carry a descriptor:
             // one should not call this method at all in that case
    public FunctionDescriptor getFunctionDescriptor() {
        assert functionDescriptor != null;
        return functionDescriptor;
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


}
