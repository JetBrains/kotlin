package org.jetbrains.jet.lang.types;

import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * @author abreslav
 */
public class FunctionDescriptorImpl extends MemberDescriptorImpl implements FunctionDescriptor {
    @NotNull
    private final List<TypeParameterDescriptor> typeParameters;
    @NotNull
    private final List<ValueParameterDescriptor> unsubstitutedValueParameters;
    @NotNull
    private final Type unsubstitutedReturnType;

    public FunctionDescriptorImpl(
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

    @Override
    @NotNull
    public List<TypeParameterDescriptor> getTypeParameters() {
        return typeParameters;
    }

    @Override
    @NotNull
    public List<ValueParameterDescriptor> getUnsubstitutedValueParameters() {
        return unsubstitutedValueParameters;
    }

    @Override
    @NotNull
    public Type getUnsubstitutedReturnType() {
        return unsubstitutedReturnType;
    }

}
