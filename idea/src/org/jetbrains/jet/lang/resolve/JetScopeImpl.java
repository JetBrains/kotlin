package org.jetbrains.jet.lang.resolve;

import org.jetbrains.jet.lang.types.*;

/**
* @author abreslav
*/
public abstract class JetScopeImpl implements JetScope {
    @Override
    public MethodDescriptor getMethods(String name) {
        return null;
    }

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
    public TypeParameterDescriptor getTypeParameterDescriptor(String name) {
        return null;
    }

    @Override
    public Type getThisType() {
        return JetStandardClasses.getNothingType();
    }
}
