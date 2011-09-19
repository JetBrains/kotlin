package org.jetbrains.jet.lang.resolve.scopes;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
* @author abreslav
*/
public abstract class JetScopeImpl implements JetScope {
    @Override
    public ClassifierDescriptor getClassifier(@NotNull String name) {
        return null;
    }

    @Override
    public VariableDescriptor getVariable(@NotNull String name) {
        return null;
    }

    @Override
    public NamespaceDescriptor getNamespace(@NotNull String name) {
        return null;
    }

    @NotNull
    @Override
    public ReceiverDescriptor getImplicitReceiver() {
        return ReceiverDescriptor.NO_RECEIVER;
    }

    @NotNull
    @Override
    public FunctionGroup getFunctionGroup(@NotNull String name) {
        return FunctionGroup.EMPTY;
    }

    @NotNull
    @Override
    public Collection<DeclarationDescriptor> getDeclarationsByLabel(String labelName) {
        return Collections.emptyList();
    }

    @Override
    public PropertyDescriptor getPropertyByFieldReference(@NotNull String fieldName) {
        return null;
    }

    @Override
    public DeclarationDescriptor getDeclarationDescriptorForUnqualifiedThis() {
        return null;
    }

    @NotNull
    @Override
    public Collection<DeclarationDescriptor> getAllDescriptors() {
        return Collections.emptyList();
    }

    @Override
    public void getImplicitReceiversHierarchy(@NotNull List<ReceiverDescriptor> result) {
    }
}
