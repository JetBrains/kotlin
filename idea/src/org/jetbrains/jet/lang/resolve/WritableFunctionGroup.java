package org.jetbrains.jet.lang.resolve;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.types.FunctionDescriptor;
import org.jetbrains.jet.lang.types.FunctionDescriptorUtil;
import org.jetbrains.jet.lang.types.FunctionGroup;
import org.jetbrains.jet.lang.types.JetType;

import java.util.ArrayList;
import java.util.List;

/**
 * @author abreslav
 */
public class WritableFunctionGroup implements FunctionGroup {
    private final String name;
    private List<FunctionDescriptor> functionDescriptors;

    public WritableFunctionGroup(String name) {
        this.name = name;
    }

    @NotNull
    @Override
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "FunctionGroup{" +
                "name='" + name + '\'' +
                '}';
    }

    public void addFunction(@NotNull FunctionDescriptor functionDescriptor) {
        getFunctionDescriptors().add(functionDescriptor);
    }

    @NotNull
    private List<FunctionDescriptor> getFunctionDescriptors() {
        if (functionDescriptors == null) {
            functionDescriptors = new ArrayList<FunctionDescriptor>();
        }
        return functionDescriptors;
    }

    @NotNull
    @Override
    public OverloadResolutionResult getPossiblyApplicableFunctions(@NotNull List<JetType> typeArguments, @NotNull List<JetType> positionedValueArgumentTypes) {
        List<FunctionDescriptor> functionDescriptors = getFunctionDescriptors();
        if (functionDescriptors.isEmpty()) return OverloadResolutionResult.nameNotFound();

        int typeArgCount = typeArguments.size();
        int valueArgCount = positionedValueArgumentTypes.size();
        List<FunctionDescriptor> result = new ArrayList<FunctionDescriptor>();
        for (FunctionDescriptor functionDescriptor : functionDescriptors) {
            // TODO : type argument inference breaks this logic
            if (functionDescriptor.getTypeParameters().size() == typeArgCount) {
                if (FunctionDescriptorUtil.getMinimumArity(functionDescriptor) <= valueArgCount &&
                        valueArgCount <= FunctionDescriptorUtil.getMaximumArity(functionDescriptor)) {
                    result.add(FunctionDescriptorUtil.substituteFunctionDescriptor(typeArguments, functionDescriptor));
                }
            }
        }
        if (result.isEmpty()) {
            assert !functionDescriptors.isEmpty();
            if (functionDescriptors.size() == 1) {
                return OverloadResolutionResult.singleFunctionArgumentMismatch(functionDescriptors.get(0));
            }
        }
        if (result.size() == 1) {
            return OverloadResolutionResult.success(result.get(0));
        }
        return OverloadResolutionResult.ambiguity(result);
    }

    @Override
    public boolean isEmpty() {
        return functionDescriptors == null || functionDescriptors.isEmpty();
    }
}
