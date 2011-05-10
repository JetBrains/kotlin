package org.jetbrains.jet.lang.descriptors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeSubstitutor;

import java.util.List;

/**
 * @author abreslav
 */
public class LocalVariableDescriptor extends VariableDescriptorImpl {
    public LocalVariableDescriptor(
            @NotNull DeclarationDescriptor containingDeclaration,
            @NotNull List<Annotation> annotations,
            @NotNull String name,
            @Nullable JetType type,
            boolean mutable) {
        super(containingDeclaration, annotations, name, mutable ? type : null, type);
    }

    @NotNull
    @Override
    public LocalVariableDescriptor substitute(TypeSubstitutor substitutor) {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    public <R, D> R accept(DeclarationDescriptorVisitor<R, D> visitor, D data) {
        return visitor.visitLocalVariableDescriptor(this, data);
    }
}
