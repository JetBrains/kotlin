package org.jetbrains.jet.lang.resolve;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.types.*;

import java.util.Collection;
import java.util.Collections;

/**
* @author abreslav
*/
public abstract class JetScopeImpl implements JetScope {
    @Override
    @NotNull
    public Collection<MethodDescriptor> getMethods(String name) {
        return Collections.emptyList();
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

    @NotNull
    @Override
    public Type getThisType() {
        return JetStandardClasses.getNothingType();
    }
}
