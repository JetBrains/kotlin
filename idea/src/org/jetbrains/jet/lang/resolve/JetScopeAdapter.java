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
    public JetType getThisType() {
        return scope.getThisType();
    }

    @NotNull
    @Override
    public FunctionGroup getFunctionGroup(@NotNull String name) {
        return scope.getFunctionGroup(name);
    }

    @Override
    public TypeParameterDescriptor getTypeParameter(@NotNull String name) {
        return scope.getTypeParameter(name);
    }

    @Override
    public NamespaceDescriptor getNamespace(@NotNull String name) {
        return scope.getNamespace(name);
    }

    @Override
    public ClassDescriptor getClass(@NotNull String name) {
        return scope.getClass(name);
    }

    @Override
    public PropertyDescriptor getProperty(@NotNull String name) {
        return scope.getProperty(name);
    }

    @Override
    public ExtensionDescriptor getExtension(@NotNull String name) {
        return scope.getExtension(name);
    }

    @NotNull
    @Override
    public DeclarationDescriptor getContainingDeclaration() {
        return scope.getContainingDeclaration();
    }
}