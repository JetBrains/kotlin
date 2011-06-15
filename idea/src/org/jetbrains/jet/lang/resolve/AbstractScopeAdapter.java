package org.jetbrains.jet.lang.resolve;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.types.JetType;

import java.util.Collection;

/**
 * @author abreslav
 */
public abstract class AbstractScopeAdapter implements JetScope {
    @NotNull
    protected abstract JetScope getWorkerScope();

    @NotNull
    @Override
    public JetType getThisType() {
        return getWorkerScope().getThisType();
    }

    @NotNull
    @Override
    public FunctionGroup getFunctionGroup(@NotNull String name) {
        return getWorkerScope().getFunctionGroup(name);
    }

    @Override
    public NamespaceDescriptor getNamespace(@NotNull String name) {
        return getWorkerScope().getNamespace(name);
    }

    @Override
    public ClassifierDescriptor getClassifier(@NotNull String name) {
        return getWorkerScope().getClassifier(name);
    }

    @Override
    public VariableDescriptor getVariable(@NotNull String name) {
        return getWorkerScope().getVariable(name);
    }

    @NotNull
    @Override
    public DeclarationDescriptor getContainingDeclaration() {
        return getWorkerScope().getContainingDeclaration();
    }

    @NotNull
    @Override
    public Collection<DeclarationDescriptor> getDeclarationsByLabel(String labelName) {
        return getWorkerScope().getDeclarationsByLabel(labelName);
    }

    @Override
    public PropertyDescriptor getPropertyByFieldReference(@NotNull String fieldName) {
        return getWorkerScope().getPropertyByFieldReference(fieldName);
    }

    @Override
    public DeclarationDescriptor getDeclarationDescriptorForUnqualifiedThis() {
        return getWorkerScope().getDeclarationDescriptorForUnqualifiedThis();
    }

    @Override
    public Collection<DeclarationDescriptor> getAllDescriptors() {
        return getWorkerScope().getAllDescriptors();
    }
}
