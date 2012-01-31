package org.jetbrains.jet.lang.resolve.java;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor;

import java.util.List;

/**
 * @author Stepan Koltsov
 */
public class TypeVariableResoverFromTypeDescriptorsInitialization extends TypeVariableByPsiResolverImpl implements TypeVariableResolver {

    @NotNull
    private final List<JavaDescriptorResolver.TypeParameterDescriptorInitialization> typeParameters;
    @Nullable
    private final TypeVariableResolver parent;

    public TypeVariableResoverFromTypeDescriptorsInitialization(@NotNull List<JavaDescriptorResolver.TypeParameterDescriptorInitialization> typeParameters, @Nullable TypeVariableResolver parent) {
        super(typeParameters, parent);
        this.typeParameters = typeParameters;
        this.parent = parent;
    }

    @NotNull
    @Override
    public TypeParameterDescriptor getTypeVariable(@NotNull String name) {
        for (JavaDescriptorResolver.TypeParameterDescriptorInitialization typeParameter : typeParameters) {
            if (typeParameter.descriptor.getName().equals(name)) {
                return typeParameter.descriptor;
            }
        }
        if (parent != null) {
            return parent.getTypeVariable(name);
        }
        throw new RuntimeException("type parameter not found by name " + name); // TODO report properly
    }
}
