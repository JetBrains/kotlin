package org.jetbrains.jet.lang.resolve;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Introduces a simple wrapper for internal scope.
 *
 * @author abreslav
 */
public abstract class AbstractScopeAdapter implements JetScope {
    @NotNull
    protected abstract JetScope getWorkerScope();

    @NotNull
    @Override
    public ReceiverDescriptor getImplicitReceiver() {
        return getWorkerScope().getImplicitReceiver();
    }

    @Override
    public void getImplicitReceiversHierarchy(@NotNull List<ReceiverDescriptor> result) {
        getWorkerScope().getImplicitReceiversHierarchy(result);
    }

    @NotNull
    @Override
    public Set<FunctionDescriptor> getFunctions(@NotNull String name) {
        return getWorkerScope().getFunctions(name);
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
    public ClassDescriptor getObjectDescriptor(@NotNull String name) {
        return getWorkerScope().getObjectDescriptor(name);
    }

    @NotNull
    @Override
    public Set<ClassDescriptor> getObjectDescriptors() {
        return getWorkerScope().getObjectDescriptors();
    }

    @NotNull
    @Override
    public Set<VariableDescriptor> getProperties(@NotNull String name) {
        return getWorkerScope().getProperties(name);
    }

    @Override
    public VariableDescriptor getLocalVariable(@NotNull String name) {
        return getWorkerScope().getLocalVariable(name);
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

    @NotNull
    @Override
    public Collection<DeclarationDescriptor> getAllDescriptors() {
        return getWorkerScope().getAllDescriptors();
    }
}
