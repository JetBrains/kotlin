package org.jetbrains.jet.lang.resolve;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.types.*;

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
    public TypeParameterDescriptor getTypeParameter(String name) {
        return scope.getTypeParameter(name);
    }

    @Override
    public NamespaceDescriptor getNamespace(String name) {
        return scope.getNamespace(name);
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

    @NotNull
    @Override
    public OverloadDomain getOverloadDomain(Type receiverType, @NotNull String referencedName) {
        return scope.getOverloadDomain(receiverType, referencedName);
    }
}
