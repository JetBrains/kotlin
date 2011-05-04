package org.jetbrains.jet.lang.types;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.JetSemanticServices;
import org.jetbrains.jet.lang.resolve.JetScope;
import org.jetbrains.jet.lang.resolve.WritableScope;

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

    private static Map<TypeConstructor, TypeProjection> createSubstitutionContext(@NotNull FunctionDescriptor functionDescriptor, List<JetType> typeArguments) {
        if (functionDescriptor.getTypeParameters().isEmpty()) return Collections.emptyMap();

        Map<TypeConstructor, TypeProjection> result = new HashMap<TypeConstructor, TypeProjection>();

        int typeArgumentsSize = typeArguments.size();
        List<TypeParameterDescriptor> typeParameters = functionDescriptor.getTypeParameters();
        assert typeArgumentsSize == typeParameters.size();
        for (int i = 0; i < typeArgumentsSize; i++) {
            TypeParameterDescriptor typeParameterDescriptor = typeParameters.get(i);
            JetType typeArgument = typeArguments.get(i);
            result.put(typeParameterDescriptor.getTypeConstructor(), new TypeProjection(typeArgument));
        }
        return result;
    }

    @Nullable
    public static List<ValueParameterDescriptor> getSubstitutedValueParameters(FunctionDescriptor substitutedDescriptor, @NotNull FunctionDescriptor functionDescriptor, TypeSubstitutor substitutor) {
        List<ValueParameterDescriptor> result = new ArrayList<ValueParameterDescriptor>();
        List<ValueParameterDescriptor> unsubstitutedValueParameters = functionDescriptor.getUnsubstitutedValueParameters();
        for (int i = 0, unsubstitutedValueParametersSize = unsubstitutedValueParameters.size(); i < unsubstitutedValueParametersSize; i++) {
            ValueParameterDescriptor unsubstitutedValueParameter = unsubstitutedValueParameters.get(i);
            // TODO : Lazy?
            JetType substitutedType = substitutor.substitute(unsubstitutedValueParameter.getOutType(), Variance.IN_VARIANCE);
            if (substitutedType == null) return null;
            result.add(new ValueParameterDescriptorImpl(
                    substitutedDescriptor,
                    i,
                    unsubstitutedValueParameter.getAnnotations(),
                    unsubstitutedValueParameter.getName(),
                    unsubstitutedValueParameter.getInType() == null ? null : substitutedType,
                    substitutedType,
                    unsubstitutedValueParameter.hasDefaultValue(),
                    unsubstitutedValueParameter.isVararg()
            ));
        }
        return result;
    }

    @Nullable
    public static JetType getSubstitutedReturnType(@NotNull FunctionDescriptor functionDescriptor, TypeSubstitutor substitutor) {
        return substitutor.substitute(functionDescriptor.getUnsubstitutedReturnType(), Variance.OUT_VARIANCE);
    }

    @Nullable
    public static FunctionDescriptor substituteFunctionDescriptor(@NotNull List<JetType> typeArguments, @NotNull FunctionDescriptor functionDescriptor) {
        Map<TypeConstructor, TypeProjection> substitutionContext = createSubstitutionContext(functionDescriptor, typeArguments);
        return functionDescriptor.substitute(TypeSubstitutor.create(substitutionContext));
    }

    @NotNull
    public static JetScope getFunctionInnerScope(@NotNull JetScope outerScope, @NotNull FunctionDescriptor descriptor, @NotNull JetSemanticServices semanticServices) {
        WritableScope parameterScope = semanticServices.createWritableScope(outerScope, descriptor);
        for (TypeParameterDescriptor typeParameter : descriptor.getTypeParameters()) {
            parameterScope.addTypeParameterDescriptor(typeParameter);
        }
        for (ValueParameterDescriptor valueParameterDescriptor : descriptor.getUnsubstitutedValueParameters()) {
            parameterScope.addVariableDescriptor(valueParameterDescriptor);
        }
        parameterScope.addLabeledDeclaration(descriptor);
        return parameterScope;
    }
}
