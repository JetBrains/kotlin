package org.jetbrains.jet.lang.resolve;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.types.FunctionDescriptor;
import org.jetbrains.jet.lang.types.FunctionGroup;
import org.jetbrains.jet.lang.types.Type;
import org.jetbrains.jet.lang.types.TypeParameterDescriptor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
                if (functionDescriptor.getMinimumArity() <= valueArgCount && valueArgCount <= functionDescriptor.getMaximumArity()) {
                    result.add(substituteFunctionDescriptor(typeArguments, functionDescriptor));
                }
            }
        }
        return result;
    }

    private FunctionDescriptor substituteFunctionDescriptor(List<Type> typeArguments, FunctionDescriptor functionDescriptor) {
        return new FunctionDescriptor(
                // TODO : substitute
                functionDescriptor.getAttributes(),
                functionDescriptor.getName(),
                Collections.<TypeParameterDescriptor>emptyList(), // TODO : questionable
                functionDescriptor.getSubstitutedValueParameters(typeArguments),
                functionDescriptor.getSubstitutedReturnType(typeArguments)
        );
    }
}
