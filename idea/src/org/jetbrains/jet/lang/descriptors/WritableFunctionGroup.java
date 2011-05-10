package org.jetbrains.jet.lang.descriptors;

import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.resolve.OverloadResolutionResult;
import org.jetbrains.jet.lang.types.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author abreslav
 */
public class WritableFunctionGroup implements FunctionGroup {
    private final String name;
    private Set<FunctionDescriptor> functionDescriptors;

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
    public Set<FunctionDescriptor> getFunctionDescriptors() {
        if (functionDescriptors == null) {
            functionDescriptors = Sets.newLinkedHashSet();
        }
        return functionDescriptors;
    }

    @NotNull
    @Override
    public OverloadResolutionResult getPossiblyApplicableFunctions(@NotNull List<JetType> typeArguments, @NotNull List<JetType> positionedValueArgumentTypes) {
        Set<FunctionDescriptor> functionDescriptors = getFunctionDescriptors();
        if (functionDescriptors.isEmpty()) return OverloadResolutionResult.nameNotFound();

        int typeArgCount = typeArguments.size();
        int valueArgCount = positionedValueArgumentTypes.size();
        List<FunctionDescriptor> result = new ArrayList<FunctionDescriptor>();
        for (FunctionDescriptor functionDescriptor : functionDescriptors) {
            // TODO : type argument inference breaks this logic
            if (functionDescriptor.getTypeParameters().size() == typeArgCount) {
                if (FunctionDescriptorUtil.getMinimumArity(functionDescriptor) <= valueArgCount &&
                        valueArgCount <= FunctionDescriptorUtil.getMaximumArity(functionDescriptor)) {
                    FunctionDescriptor substitutedFunctionDescriptor = FunctionDescriptorUtil.substituteFunctionDescriptor(typeArguments, functionDescriptor);
                    assert substitutedFunctionDescriptor != null;
                    result.add(substitutedFunctionDescriptor);
                }
            }
        }
        if (result.isEmpty()) {
            assert !functionDescriptors.isEmpty();
            if (functionDescriptors.size() == 1) {
                return OverloadResolutionResult.singleFunctionArgumentMismatch(functionDescriptors.iterator().next());
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
