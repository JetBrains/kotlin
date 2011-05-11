package org.jetbrains.jet.lang.descriptors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.types.NamespaceType;
import org.jetbrains.jet.lang.types.TypeSubstitutor;

import java.util.List;

/**
 * @author abreslav
 */
public abstract class AbstractNamespaceDescriptorImpl extends DeclarationDescriptorImpl implements NamespaceDescriptor {
    private NamespaceType namespaceType;

    public AbstractNamespaceDescriptorImpl(DeclarationDescriptor containingDeclaration, List<Annotation> annotations, String name) {
        super(containingDeclaration, annotations, name);
    }

    @Override
    @NotNull
    public NamespaceType getNamespaceType() {
        if (namespaceType == null) {
            namespaceType = new NamespaceType(getName(), getMemberScope());
        }
        return namespaceType;
    }

    @NotNull
    @Override
    public NamespaceDescriptor substitute(TypeSubstitutor substitutor) {
        throw new UnsupportedOperationException("This operation does not make sense for a namespace");
    }

    @Override
    public <R, D> R accept(DeclarationDescriptorVisitor<R, D> visitor, D data) {
        return visitor.visitNamespaceDescriptor(this, data);
    }
}
