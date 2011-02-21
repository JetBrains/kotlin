package org.jetbrains.jet.lang.resolve;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.types.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author abreslav
 */
public class WritableFunctionGroup implements FunctionGroup {
    private final String name;
    private Collection<FunctionDescriptor> functionDescriptors;

    public WritableFunctionGroup(String name) {
        this.name = name;
    }

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
    private Collection<FunctionDescriptor> getFunctionDescriptors() {
        if (functionDescriptors == null) {
            functionDescriptors = new ArrayList<FunctionDescriptor>();
        }
        return functionDescriptors;
    }

    @NotNull
    @Override
    public Collection<FunctionDescriptor> getPossiblyApplicableFunctions(@NotNull List<Type> typeArguments, @NotNull List<Type> positionedValueArgumentTypes) {
        int typeArgCount = typeArguments.size();
        int valueArgCount = positionedValueArgumentTypes.size();
        Collection<FunctionDescriptor> result = new ArrayList<FunctionDescriptor>();
        for (FunctionDescriptor functionDescriptor : getFunctionDescriptors()) {
            // TODO : type argument inference breaks this logic
            if (functionDescriptor.getTypeParameters().size() == typeArgCount) {
                if (FunctionDescriptorUtil.getMinimumArity(functionDescriptor) <= valueArgCount && valueArgCount <= FunctionDescriptorUtil.getMaximumArity(functionDescriptor)) {
                    result.add(FunctionDescriptorUtil.substituteFunctionDescriptor(typeArguments, functionDescriptor));
                }
            }
        }
        return result;
    }

    @Override
    public boolean isEmpty() {
        return functionDescriptors.isEmpty();
    }
}
