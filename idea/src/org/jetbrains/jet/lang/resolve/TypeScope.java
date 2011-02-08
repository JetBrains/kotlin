package org.jetbrains.jet.lang.resolve;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.types.*;

import java.util.Collection;

/**
 * @author abreslav
 */
public class TypeScope implements JetScope {
    private final Type receiverType;

    public TypeScope(Type receiverType) {
        this.receiverType = receiverType;
    }

    @Override
    public TypeParameterDescriptor getTypeParameterDescriptor(String name) {
        return null;
    }

    @NotNull
    @Override
    public Type getThisType() {
        return receiverType;
    }

    @Override
    public Collection<MethodDescriptor> getMethods(String name) {
        return receiverType.getMemberDomain().getMethods(receiverType, name);
    }

    @Override
    public ClassDescriptor getClass(String name) {
        return receiverType.getMemberDomain().getClassDescriptor(receiverType, name);
    }

    @Override
    public PropertyDescriptor getProperty(String name) {
        return receiverType.getMemberDomain().getProperty(receiverType, name);
    }

    @Override
    public ExtensionDescriptor getExtension(String name) {
        return receiverType.getMemberDomain().getExtension(receiverType, name);
    }

    @Override
    public NamespaceDescriptor getNamespace(String name) {
        return null;
    }
}
