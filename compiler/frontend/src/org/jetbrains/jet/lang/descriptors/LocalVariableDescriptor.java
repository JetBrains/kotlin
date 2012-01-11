package org.jetbrains.jet.lang.descriptors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeSubstitutor;

import java.util.List;

/**
 * @author abreslav
 */
public class LocalVariableDescriptor extends VariableDescriptorImpl {
    private boolean isVar;
    public LocalVariableDescriptor(
            @NotNull DeclarationDescriptor containingDeclaration,
            @NotNull List<AnnotationDescriptor> annotations,
            @NotNull String name,
            @Nullable JetType type,
            boolean mutable) {
        super(containingDeclaration, annotations, name, type);
        isVar = mutable;
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

    @Override
    public boolean isVar() {
        return isVar;
    }

    @Override
    public boolean isObjectDeclaration() {
        return false;
    }
}
