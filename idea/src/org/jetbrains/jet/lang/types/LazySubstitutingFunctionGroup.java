package org.jetbrains.jet.lang.types;

import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.resolve.OverloadResolutionResult;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * @author abreslav
 */
public class LazySubstitutingFunctionGroup implements FunctionGroup {
    private final TypeSubstitutor substitutor;
    private final FunctionGroup functionGroup;
    private Set<FunctionDescriptor> functionDescriptors;

    public LazySubstitutingFunctionGroup(TypeSubstitutor substitutor, FunctionGroup functionGroup) {
        this.substitutor = substitutor;
        this.functionGroup = functionGroup;
    }

    @NotNull
    @Override
    public String getName() {
        return functionGroup.getName();
    }

    @NotNull
    @Override
    public OverloadResolutionResult getPossiblyApplicableFunctions(@NotNull List<JetType> typeArguments, @NotNull List<JetType> positionedValueArgumentTypes) {
        OverloadResolutionResult resolutionResult = functionGroup.getPossiblyApplicableFunctions(typeArguments, positionedValueArgumentTypes);
        if (resolutionResult.isNothing()) return resolutionResult;

        Collection<FunctionDescriptor> result = new ArrayList<FunctionDescriptor>();
        for (FunctionDescriptor function : resolutionResult.getFunctionDescriptors()) {
            FunctionDescriptor functionDescriptor = substitute(function);
            if (functionDescriptor != null) {
                result.add(functionDescriptor);
            }
        }
        return resolutionResult.newContents(result);
    }

    @Nullable
    private FunctionDescriptor substitute(
            @NotNull FunctionDescriptor functionDescriptor) {
        if (substitutor.isEmpty()) return functionDescriptor;

        return functionDescriptor.substitute(substitutor);
    }

    @Override
    public boolean isEmpty() {
        return functionGroup.isEmpty();
    }

    @NotNull
    @Override
    public Set<FunctionDescriptor> getFunctionDescriptors() {
        if (functionDescriptors == null) {
            functionDescriptors = Sets.newHashSet();
            for (FunctionDescriptor descriptor : functionGroup.getFunctionDescriptors()) {
                functionDescriptors.add(descriptor.substitute(substitutor));
            }
        }
        return functionDescriptors;
    }
}
