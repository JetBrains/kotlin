package org.jetbrains.jet.lang.types;

import org.jetbrains.annotations.NotNull;

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

    public LazySubstitutingFunctionGroup(Map<TypeConstructor, TypeProjection> substitutionContext, FunctionGroup functionGroup) {
        this.substitutionContext = substitutionContext;
        this.functionGroup = functionGroup;
    }

    @NotNull
    @Override
    public String getName() {
        return functionGroup.getName();
    }

    @NotNull
    @Override
    public Collection<FunctionDescriptor> getPossiblyApplicableFunctions(@NotNull List<Type> typeArguments, @NotNull List<Type> positionedValueArgumentTypes) {
        Collection<FunctionDescriptor> possiblyApplicableFunctions = functionGroup.getPossiblyApplicableFunctions(typeArguments, positionedValueArgumentTypes);
        Collection<FunctionDescriptor> result = new ArrayList<FunctionDescriptor>();
        for (FunctionDescriptor function : possiblyApplicableFunctions) {
            result.add(new LazySubstitutingFunctionDescriptor(substitutionContext, function));
        }
        return result;
    }

    @Override
    public boolean isEmpty() {
        return functionGroup.isEmpty();
    }
}
