package org.jetbrains.jet.lang.resolve.scopes;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.SetMultimap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.jetbrains.jet.util.CommonSuppliers;

import java.util.*;

/**
 * @author svtk
 */
public class ScopeBoundToReceiver implements JetScope {
    private final DeclarationDescriptor receiver;
    private final JetScope scope;
    
    private SetMultimap<String, VariableDescriptor> propertiesMap;
    private SetMultimap<String, FunctionDescriptor> functionsMap;
    private Map<DeclarationDescriptor, DeclarationDescriptor> descriptorsBoundToReceiver;

    public static ScopeBoundToReceiver create(DeclarationDescriptor implicitReceiver, JetScope memberScope) {
        return new ScopeBoundToReceiver(implicitReceiver, memberScope);
    }

    private ScopeBoundToReceiver(DeclarationDescriptor receiver, JetScope scope) {
        this.receiver = receiver;
        this.scope = scope;
    }

    public DeclarationDescriptor getReceiver() {
        return receiver;
    }
    
    private SetMultimap<String, VariableDescriptor> getPropertiesMap() {
        if (propertiesMap == null) {
            propertiesMap = CommonSuppliers.newLinkedHashSetHashSetMultimap();
        }
        return propertiesMap;
    }

    private SetMultimap<String, FunctionDescriptor> getFunctionsMap() {
        if (functionsMap == null) {
            functionsMap = CommonSuppliers.newLinkedHashSetHashSetMultimap();
        }
        return functionsMap;
    }
    
    private Map<DeclarationDescriptor, DeclarationDescriptor> getDescriptorsBoundToReceiver() {
        if (descriptorsBoundToReceiver == null) {
            descriptorsBoundToReceiver = Maps.newHashMap();
        }
        return descriptorsBoundToReceiver;
    }

    @Nullable
    @Override
    public ClassifierDescriptor getClassifier(@NotNull String name) {
        return scope.getClassifier(name);
    }

    @Nullable
    @Override
    public NamespaceDescriptor getNamespace(@NotNull String name) {
        return scope.getNamespace(name);
    }

    @NotNull
    @Override
    public Set<VariableDescriptor> getProperties(@NotNull String name) {
        if (getPropertiesMap().containsKey(name)) {
            return getPropertiesMap().get(name);
        }
        return getMembersBoundToReceiver(name, getPropertiesMap(), scope.getProperties(name), new Function<VariableDescriptor, VariableDescriptor>() {
            @Override
            public VariableDescriptor apply(VariableDescriptor property) {
                return new VariableDescriptorBoundToReceiver(property, receiver);
            }
        });
    }

    @Nullable
    @Override
    public VariableDescriptor getLocalVariable(@NotNull String name) {
        return scope.getLocalVariable(name);
    }
    
    private <D extends DeclarationDescriptor> Set<D> getMembersBoundToReceiver(String name, SetMultimap<String, D> cache, Set<D> oldMembers, Function<D, D> transformation) {
        for (D oldMember : oldMembers) {
            cache.put(name, transformation.apply(oldMember));
        }
        return cache.get(name);
    }

    @NotNull
    @Override
    public Set<FunctionDescriptor> getFunctions(@NotNull String name) {
        if (getFunctionsMap().containsKey(name)) {
            return getFunctionsMap().get(name);
        }
        return getMembersBoundToReceiver(name, getFunctionsMap(), scope.getFunctions(name), new Function<FunctionDescriptor, FunctionDescriptor>() {
            @Override
            public FunctionDescriptor apply(FunctionDescriptor function) {
                return new FunctionDescriptorBoundToReceiver(function, receiver);
            }
        });
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

    @Nullable
    @Override
    public PropertyDescriptor getPropertyByFieldReference(@NotNull String fieldName) {
        return scope.getPropertyByFieldReference(fieldName);
    }

    @NotNull
    @Override
    public Collection<DeclarationDescriptor> getAllDescriptors() {
        Collection<DeclarationDescriptor> result = Lists.newArrayList();
        Collection<DeclarationDescriptor> allDescriptors = scope.getAllDescriptors();
        for (DeclarationDescriptor descriptor : allDescriptors) {
            if (getDescriptorsBoundToReceiver().containsKey(descriptor)) {
                result.add(getDescriptorsBoundToReceiver().get(descriptor));
                continue;
            }
            if (descriptor instanceof FunctionDescriptor) {
                getFunctions(descriptor.getName());
            }
            else if (descriptor instanceof VariableDescriptor) {
                getProperties(descriptor.getName());
            }
            else {
                result.add(descriptor);
                continue;
            }
            if (getDescriptorsBoundToReceiver().containsKey(descriptor)) {
                result.add(getDescriptorsBoundToReceiver().get(descriptor));
            }
        }
        return result;
    }

    @NotNull
    @Override
    public ReceiverDescriptor getImplicitReceiver() {
        return ReceiverDescriptor.NO_RECEIVER;
    }

    @Override
    public void getImplicitReceiversHierarchy(@NotNull List<ReceiverDescriptor> result) {
    }
}
