package org.jetbrains.jet.lang.resolve.scopes;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.jetbrains.jet.lang.types.TypeSubstitutor;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author abreslav
 */
public class SubstitutingScope implements JetScope {

    private final JetScope workerScope;
    private final TypeSubstitutor substitutor;

    private Map<String, FunctionGroup> functionGroups = null;
    private Collection<DeclarationDescriptor> allDescriptors = null;

    public SubstitutingScope(JetScope workerScope, @NotNull TypeSubstitutor substitutor) {
        this.workerScope = workerScope;
        this.substitutor = substitutor;
    }

    @Override
    public VariableDescriptor getVariable(@NotNull String name) {
        VariableDescriptor variable = workerScope.getVariable(name);
        if (variable == null || substitutor.isEmpty()) {
            return variable;
        }

        return variable.substitute(substitutor);
    }

    @Override
    public ClassifierDescriptor getClassifier(@NotNull String name) {
        ClassifierDescriptor descriptor = workerScope.getClassifier(name);
        if (descriptor == null) {
            return null;
        }
        if (descriptor instanceof ClassDescriptor) {
            return new LazySubstitutingClassDescriptor((ClassDescriptor) descriptor, substitutor);
        }
        throw new UnsupportedOperationException(); // TODO
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
    public FunctionGroup getFunctionGroup(@NotNull String name) {
        if (substitutor.isEmpty()) {
            return workerScope.getFunctionGroup(name);
        }
        if (functionGroups == null) {
            functionGroups = Maps.newHashMap();
        }
        FunctionGroup cachedGroup = functionGroups.get(name);
        if (cachedGroup != null) {
            return cachedGroup;
        }

        FunctionGroup functionGroup = workerScope.getFunctionGroup(name);
        FunctionGroup result;
        if (functionGroup.isEmpty()) {
            result = FunctionGroup.EMPTY;
        }
        else {
            result = new LazySubstitutingFunctionGroup(substitutor, functionGroup);
        }
        functionGroups.put(name, result);
        return result;
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
                DeclarationDescriptor substitute = descriptor.substitute(substitutor);
                assert substitute != null;
                allDescriptors.add(substitute);
            }
        }
        return allDescriptors;
    }
}
