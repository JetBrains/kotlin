package org.jetbrains.jet.lang.types;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.resolve.OverloadResolutionResult;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author abreslav
 */
public class LazySubstitutingFunctionGroup implements FunctionGroup {
    private final Map<TypeConstructor, TypeProjection> substitutionContext;
    private final FunctionGroup functionGroup;
    private final TypeSubstitutor typeSubstitutor;

    public LazySubstitutingFunctionGroup(Map<TypeConstructor, TypeProjection> substitutionContext, FunctionGroup functionGroup, TypeSubstitutor typeSubstitutor) {
        this.substitutionContext = substitutionContext;
        this.functionGroup = functionGroup;
        this.typeSubstitutor = typeSubstitutor;
    }

    public LazySubstitutingFunctionGroup(Map<TypeConstructor, TypeProjection> substitutionContext, FunctionGroup functionGroup) {
        this(substitutionContext, functionGroup, TypeSubstitutor.INSTANCE);
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
            FunctionDescriptor functionDescriptor = substitute(substitutionContext, function, typeSubstitutor);
            if (functionDescriptor != null) {
                result.add(functionDescriptor);
            }
        }
        return resolutionResult.newContents(result);
    }

    @Nullable
    public FunctionDescriptor substitute(
            @NotNull Map<TypeConstructor, TypeProjection> substitutionContext,
            @NotNull FunctionDescriptor functionDescriptor, TypeSubstitutor typeSubstitutor) {
        if (substitutionContext.isEmpty()) return functionDescriptor;

        FunctionDescriptor substituted = FunctionDescriptorUtil.substituteFunctionDescriptor(functionDescriptor, substitutionContext, typeSubstitutor);
        if (substituted == null) {
            return null;
        }
        return substituted;
    }

    @Override
    public boolean isEmpty() {
        return functionGroup.isEmpty();
    }
}
