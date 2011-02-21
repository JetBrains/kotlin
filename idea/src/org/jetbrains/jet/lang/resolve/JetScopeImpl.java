package org.jetbrains.jet.lang.resolve;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.types.*;

/**
* @author abreslav
*/
public abstract class JetScopeImpl implements JetScope {
    @Override
    public ClassDescriptor getClass(String name) {
        return null;
    }

    @Override
    public PropertyDescriptor getProperty(String name) {
        return null;
    }

    @Override
    public ExtensionDescriptor getExtension(String name) {
        return null;
    }

    @Override
    public NamespaceDescriptor getNamespace(String name) {
        return null;
    }

    @Override
    public TypeParameterDescriptor getTypeParameter(String name) {
        return null;
    }

    @NotNull
    @Override
    public Type getThisType() {
        return JetStandardClasses.getNothingType();
    }

    @NotNull
    @Override
    public OverloadDomain getOverloadDomain(Type receiverType, @NotNull String referencedName) {
        return receiverType.getMemberScope().getOverloadDomain(null, referencedName);
    }
}
