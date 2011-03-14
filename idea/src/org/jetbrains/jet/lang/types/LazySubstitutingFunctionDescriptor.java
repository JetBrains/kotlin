package org.jetbrains.jet.lang.types;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author abreslav
 */
public class LazySubstitutingFunctionDescriptor implements FunctionDescriptor {
    private final Map<TypeConstructor, TypeProjection> substitutionContext;
    private final FunctionDescriptor functionDescriptor;

    public LazySubstitutingFunctionDescriptor(Map<TypeConstructor, TypeProjection> substitutionContext, FunctionDescriptor functionDescriptor) {
        this.substitutionContext = substitutionContext;
        this.functionDescriptor = functionDescriptor;
    }

    @NotNull
    @Override
    public List<TypeParameterDescriptor> getTypeParameters() {
        List<TypeParameterDescriptor> result = new ArrayList<TypeParameterDescriptor>();
        for (TypeParameterDescriptor parameterDescriptor : functionDescriptor.getTypeParameters()) {
            // TODO : lazy?
            result.add(new TypeParameterDescriptor(
                    this,
                    parameterDescriptor.getAttributes(),
                    parameterDescriptor.getVariance(),
                    parameterDescriptor.getName(),
                    TypeSubstitutor.INSTANCE.substituteInSet(substitutionContext, parameterDescriptor.getUpperBounds(), Variance.INVARIANT)));
        }
        return result;
    }

    @NotNull
    @Override
    public List<ValueParameterDescriptor> getUnsubstitutedValueParameters() {
        List<ValueParameterDescriptor> result = new ArrayList<ValueParameterDescriptor>();
        List<ValueParameterDescriptor> unsubstitutedValueParameters = functionDescriptor.getUnsubstitutedValueParameters();
        for (int i = 0, unsubstitutedValueParametersSize = unsubstitutedValueParameters.size(); i < unsubstitutedValueParametersSize; i++) {
            ValueParameterDescriptor parameterDescriptor = unsubstitutedValueParameters.get(i);
            result.add(new ValueParameterDescriptorImpl(
                    this,
                    i,
                    parameterDescriptor.getAttributes(),
                    parameterDescriptor.getName(),
                    TypeSubstitutor.INSTANCE.substitute(substitutionContext, parameterDescriptor.getType(), Variance.IN_VARIANCE),
                    parameterDescriptor.hasDefaultValue(),
                    parameterDescriptor.isVararg()
            ));
        }
        return result;
    }

    @NotNull
    @Override
    public Type getUnsubstitutedReturnType() {
        return TypeSubstitutor.INSTANCE.substitute(substitutionContext, functionDescriptor.getUnsubstitutedReturnType(), Variance.OUT_VARIANCE);
    }

    @NotNull
    @Override
    public FunctionDescriptor getOriginal() {
        return functionDescriptor.getOriginal();
    }

    @Override
    public List<Attribute> getAttributes() {
        // TODO : Substitute?
        return functionDescriptor.getAttributes();
    }

    @Override
    public String getName() {
        return functionDescriptor.getName();
    }

    @NotNull
    @Override
    public DeclarationDescriptor getContainingDeclaration() {
        return functionDescriptor.getContainingDeclaration();
    }

    @Override
    public <R, D> R accept(DeclarationDescriptorVisitor<R, D> visitor, D data) {
        return visitor.visitFunctionDescriptor(this, data);
    }

    @Override
    public void acceptVoid(DeclarationDescriptorVisitor<Void, Void> visitor) {
        accept(visitor, null);
    }
}

