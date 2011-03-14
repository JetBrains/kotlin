package org.jetbrains.jet.lang.types;

import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * @author abreslav
 */
public class FunctionDescriptorUtil {
    /** @return Minimal number of arguments to be passed */
    public static int getMinimumArity(@NotNull FunctionDescriptor functionDescriptor) {
        int result = 0;
        for (ValueParameterDescriptor valueParameter : functionDescriptor.getUnsubstitutedValueParameters()) {
            if (valueParameter.hasDefaultValue()) {
                break;
            }
            result++;
        }
        return result;
    }

    /**
     * @return Maximum number of arguments that can be passed. -1 if unbound (vararg)
     */
    public static int getMaximumArity(@NotNull FunctionDescriptor functionDescriptor) {
        List<ValueParameterDescriptor> unsubstitutedValueParameters = functionDescriptor.getUnsubstitutedValueParameters();
        if (unsubstitutedValueParameters.isEmpty()) {
            return 0;
        }
        // TODO : check somewhere that vararg is only the last one, and that varargs do not have default values

        ValueParameterDescriptor lastParameter = unsubstitutedValueParameters.get(unsubstitutedValueParameters.size() - 1);
        if (lastParameter.isVararg()) {
            return -1;
        }
        return unsubstitutedValueParameters.size();
    }

    @NotNull
    public static List<ValueParameterDescriptor> getSubstitutedValueParameters(FunctionDescriptor substitutedDescriptor, @NotNull FunctionDescriptor functionDescriptor, @NotNull List<Type> typeArguments) {
        List<ValueParameterDescriptor> result = new ArrayList<ValueParameterDescriptor>();
        Map<TypeConstructor, TypeProjection> context = createSubstitutionContext(functionDescriptor, typeArguments);
        List<ValueParameterDescriptor> unsubstitutedValueParameters = functionDescriptor.getUnsubstitutedValueParameters();
        for (int i = 0, unsubstitutedValueParametersSize = unsubstitutedValueParameters.size(); i < unsubstitutedValueParametersSize; i++) {
            ValueParameterDescriptor unsubstitutedValueParameter = unsubstitutedValueParameters.get(i);
            // TODO : Lazy?
            result.add(new ValueParameterDescriptorImpl(
                    substitutedDescriptor,
                    i,
                    unsubstitutedValueParameter.getAttributes(),
                    unsubstitutedValueParameter.getName(),
                    TypeSubstitutor.INSTANCE.substitute(context, unsubstitutedValueParameter.getType(), Variance.IN_VARIANCE),
                    unsubstitutedValueParameter.hasDefaultValue(),
                    unsubstitutedValueParameter.isVararg()
            ));
        }
        return result;
    }

    private static Map<TypeConstructor, TypeProjection> createSubstitutionContext(@NotNull FunctionDescriptor functionDescriptor, List<Type> typeArguments) {
        Map<TypeConstructor,TypeProjection> result = new HashMap<TypeConstructor, TypeProjection>();

        int typeArgumentsSize = typeArguments.size();
        List<TypeParameterDescriptor> typeParameters = functionDescriptor.getTypeParameters();
        assert typeArgumentsSize == typeParameters.size();
        for (int i = 0; i < typeArgumentsSize; i++) {
            TypeParameterDescriptor typeParameterDescriptor = typeParameters.get(i);
            Type typeArgument = typeArguments.get(i);
            result.put(typeParameterDescriptor.getTypeConstructor(), new TypeProjection(typeArgument));
        }
        return result;
    }

    @NotNull
    public static Type getSubstitutedReturnType(@NotNull FunctionDescriptor functionDescriptor, @NotNull List<Type> typeArguments) {
        return TypeSubstitutor.INSTANCE.substitute(createSubstitutionContext(functionDescriptor, typeArguments), functionDescriptor.getUnsubstitutedReturnType(), Variance.OUT_VARIANCE);
    }

    @NotNull
    public static FunctionDescriptor substituteFunctionDescriptor(@NotNull List<Type> typeArguments, @NotNull FunctionDescriptor functionDescriptor) {
        if (functionDescriptor.getTypeParameters().isEmpty()) {
            return functionDescriptor;
        }
        FunctionDescriptorImpl substitutedDescriptor = new FunctionDescriptorImpl(
                functionDescriptor,
                // TODO : substitute
                functionDescriptor.getAttributes(),
                functionDescriptor.getName());
        substitutedDescriptor.initialize(
                Collections.<TypeParameterDescriptor>emptyList(), // TODO : questionable
                getSubstitutedValueParameters(substitutedDescriptor, functionDescriptor, typeArguments),
                getSubstitutedReturnType(functionDescriptor, typeArguments)
        );
        return substitutedDescriptor;
    }
}
