package org.jetbrains.jet.lang.resolve.scopes;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.jetbrains.jet.lang.types.TypeSubstitutor;

import java.util.*;

/**
 * @author abreslav
 */
public class SubstitutingScope implements JetScope {

    private final JetScope workerScope;
    private final TypeSubstitutor substitutor;

    private Map<DeclarationDescriptor, DeclarationDescriptor> substitutedDescriptors = null;
    private Collection<DeclarationDescriptor> allDescriptors = null;

    public SubstitutingScope(JetScope workerScope, @NotNull TypeSubstitutor substitutor) {
        this.workerScope = workerScope;
        this.substitutor = substitutor;
    }

    @Nullable
    private <D extends DeclarationDescriptor> D substitute(@Nullable D descriptor) {
        if (descriptor == null) return null;
        if (substitutor.isEmpty()) return descriptor;

        if (substitutedDescriptors == null) {
            substitutedDescriptors = Maps.newHashMap();
        }

        DeclarationDescriptor substituted = substitutedDescriptors.get(descriptor);
        if (substituted == null) {
            substituted = descriptor.substitute(substitutor);
            substitutedDescriptors.put(descriptor, substituted);
        }
        //noinspection unchecked
        return (D) substituted;
    }

    @NotNull
    private <D extends DeclarationDescriptor> Set<D> substitute(@NotNull Set<D> descriptors) {
        if (substitutor.isEmpty()) return descriptors;
        if (descriptors.isEmpty()) return descriptors;

        Set<D> result = Sets.newHashSet();
        for (D descriptor : descriptors) {
            D substitute = substitute(descriptor);
            if (substitute != null) {
                result.add(substitute);
            }
        }

        return result;
    }

    @Override
    public VariableDescriptor getVariable(@NotNull String name) {
        return substitute(workerScope.getVariable(name));
    }

    @Override
    public ClassifierDescriptor getClassifier(@NotNull String name) {
        return substitute(workerScope.getClassifier(name));
    }

    @NotNull
    @Override
    public Set<FunctionDescriptor> getFunctions(@NotNull String name) {
        return substitute(workerScope.getFunctions(name));
    }

    @Override
    public NamespaceDescriptor getNamespace(@NotNull String name) {
        return workerScope.getNamespace(name); // TODO
    }

    @NotNull
    @Override
    public ReceiverDescriptor getImplicitReceiver() {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    public void getImplicitReceiversHierarchy(@NotNull List<ReceiverDescriptor> result) {
        throw new UnsupportedOperationException(); // TODO
    }

    @NotNull
    @Override
    public DeclarationDescriptor getContainingDeclaration() {
        return workerScope.getContainingDeclaration();
    }

    @NotNull
    @Override
    public Collection<DeclarationDescriptor> getDeclarationsByLabel(String labelName) {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    public PropertyDescriptor getPropertyByFieldReference(@NotNull String fieldName) {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    @Nullable
    public DeclarationDescriptor getDeclarationDescriptorForUnqualifiedThis() {
        return workerScope.getDeclarationDescriptorForUnqualifiedThis();
    }

    @NotNull
    @Override
    public Collection<DeclarationDescriptor> getAllDescriptors() {
        if (allDescriptors == null) {
            allDescriptors = Sets.newHashSet();
            for (DeclarationDescriptor descriptor : workerScope.getAllDescriptors()) {
                DeclarationDescriptor substitute = substitute(descriptor);
                assert substitute != null;
                allDescriptors.add(substitute);
            }
        }
        return allDescriptors;
    }
}
