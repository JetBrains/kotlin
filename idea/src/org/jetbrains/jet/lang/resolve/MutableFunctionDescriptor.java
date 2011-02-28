package org.jetbrains.jet.lang.resolve;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.types.FunctionDescriptor;
import org.jetbrains.jet.lang.types.Type;
import org.jetbrains.jet.lang.types.TypeParameterDescriptor;
import org.jetbrains.jet.lang.types.ValueParameterDescriptor;

import java.util.List;

/**
 * @author abreslav
 */
public class MutableFunctionDescriptor extends MutableDeclarationDescriptor implements FunctionDescriptor {
    @NotNull
    @Override
    public List<TypeParameterDescriptor> getTypeParameters() {
        throw new UnsupportedOperationException(); // TODO
    }

    @NotNull
    @Override
    public List<ValueParameterDescriptor> getUnsubstitutedValueParameters() {
        throw new UnsupportedOperationException(); // TODO
    }

    @NotNull
    @Override
    public Type getUnsubstitutedReturnType() {
        throw new UnsupportedOperationException(); // TODO
    }
}
