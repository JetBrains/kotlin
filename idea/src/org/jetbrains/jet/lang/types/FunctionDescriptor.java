package org.jetbrains.jet.lang.types;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.resolve.TypeResolver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author abreslav
 */
public class FunctionDescriptor extends MemberDescriptorImpl {
    @NotNull
    private final List<TypeParameterDescriptor> typeParameters;
    @NotNull
    private final List<ValueParameterDescriptor> unsubstitutedValueParameters;
    @NotNull
    private final Type unsubstitutedReturnType;

    public FunctionDescriptor(
            @NotNull List<Attribute> attributes,
            String name,
            @NotNull List<TypeParameterDescriptor> typeParameters,
            @NotNull List<ValueParameterDescriptor> unsubstitutedValueParameters,
            @NotNull Type unsubstitutedReturnType) {
        super(attributes, name);
        this.typeParameters = typeParameters;
        this.unsubstitutedValueParameters = unsubstitutedValueParameters;
        this.unsubstitutedReturnType = unsubstitutedReturnType;
    }

    @NotNull
    public List<TypeParameterDescriptor> getTypeParameters() {
        return typeParameters;
    }

    @NotNull
    public List<ValueParameterDescriptor> getUnsubstitutedValueParameters() {
        return unsubstitutedValueParameters;
    }

    @NotNull
    public Type getUnsubstitutedReturnType() {
        return unsubstitutedReturnType;
    }

    /** @return Minimal number of arguments to be passed */
    public int getMinimumArity() {
        int result = 0;
        for (ValueParameterDescriptor valueParameter : unsubstitutedValueParameters) {
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
    public int getMaximumArity() {
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
    public List<ValueParameterDescriptor> getSubstitutedValueParameters(@NotNull List<Type> typeArguments) {
        List<ValueParameterDescriptor> result = new ArrayList<ValueParameterDescriptor>();
        Map<TypeConstructor,TypeProjection> context = createSubstitutionContext(typeArguments);
        for (ValueParameterDescriptor unsubstitutedValueParameter : unsubstitutedValueParameters) {
            // TODO : Lazy?
            result.add(new ValueParameterDescriptorImpl(
                    unsubstitutedValueParameter.getAttributes(),
                    unsubstitutedValueParameter.getName(),
                    TypeSubstitutor.INSTANCE.substitute(context, unsubstitutedValueParameter.getType(), Variance.IN_VARIANCE),
                    unsubstitutedValueParameter.hasDefaultValue()
            ));
        }
        return result;
    }

    private Map<TypeConstructor, TypeProjection> createSubstitutionContext(List<Type> typeArguments) {
        Map<TypeConstructor,TypeProjection> result = new HashMap<TypeConstructor, TypeProjection>();

        int typeArgumentsSize = typeArguments.size();
        assert typeArgumentsSize == typeParameters.size();
        for (int i = 0; i < typeArgumentsSize; i++) {
            TypeParameterDescriptor typeParameterDescriptor = typeParameters.get(i);
            Type typeArgument = typeArguments.get(i);
            result.put(typeParameterDescriptor.getTypeConstructor(), new TypeProjection(typeArgument));
        }
        return result;
    }

    @NotNull
    public Type getSubstitutedReturnType(@NotNull List<Type> typeArguments) {
        return TypeSubstitutor.INSTANCE.substitute(createSubstitutionContext(typeArguments), unsubstitutedReturnType, Variance.OUT_VARIANCE);
    }
}
