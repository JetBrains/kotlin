package org.jetbrains.jet.lang.resolve;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.types.*;

import java.util.Collection;

/**
 * @author abreslav
 */
public class JetScopeAdapter implements JetScope {
    private final JetScope scope;

    public JetScopeAdapter(JetScope scope) {
        this.scope = scope;
    }

    @NotNull
    @Override
    public Type getThisType() {
        return scope.getThisType();
    }

    @Override
    public TypeParameterDescriptor getTypeParameterDescriptor(String name) {
        return scope.getTypeParameterDescriptor(name);
    }

    @Override
    public NamespaceDescriptor getNamespace(String name) {
        return scope.getNamespace(name);
    }

    @Override
    public Collection<MethodDescriptor> getMethods(String name) {
        return scope.getMethods(name);
    }

    @Override
    public ClassDescriptor getClass(String name) {
        return scope.getClass(name);
    }

    @Override
    public PropertyDescriptor getProperty(String name) {
        return scope.getProperty(name);
    }

    @Override
    public ExtensionDescriptor getExtension(String name) {
        return scope.getExtension(name);
    }
}
