package org.jetbrains.jet.lang.resolve;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.types.*;

import java.util.Collection;

/**
 * @author abreslav
 */
public class JetScopeAdapter implements JetScope {
    @NotNull
    private final JetScope scope;

    public JetScopeAdapter(@NotNull JetScope scope) {
        this.scope = scope;
    }

    @NotNull
    protected final JetScope getWorkerScope() {
        return scope;
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
    public NamespaceDescriptor getNamespace(@NotNull String name) {
        return scope.getNamespace(name);
    }

    @Override
    public ClassifierDescriptor getClassifier(@NotNull String name) {
        return scope.getClassifier(name);
    }

    @Override
    public VariableDescriptor getVariable(@NotNull String name) {
        return scope.getVariable(name);
    }

    @NotNull
    @Override
    public DeclarationDescriptor getContainingDeclaration() {
        return scope.getContainingDeclaration();
    }

    @NotNull
    @Override
    public Collection<DeclarationDescriptor> getDeclarationsByLabel(String labelName) {
        return scope.getDeclarationsByLabel(labelName);
    }

    @Override
    public PropertyDescriptor getPropertyByFieldReference(@NotNull String fieldName) {
        return scope.getPropertyByFieldReference(fieldName);
    }
}