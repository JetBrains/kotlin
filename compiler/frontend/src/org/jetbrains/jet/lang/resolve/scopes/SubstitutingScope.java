/*
 * Copyright 2010-2012 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.jet.lang.resolve.scopes;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.resolve.name.LabelName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.jetbrains.jet.lang.types.TypeSubstitutor;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    @NotNull
    @Override
    public Set<VariableDescriptor> getProperties(@NotNull Name name) {
        return substitute(workerScope.getProperties(name));
    }

    @Override
    public VariableDescriptor getLocalVariable(@NotNull Name name) {
        return substitute(workerScope.getLocalVariable(name));
    }

    @Override
    public ClassifierDescriptor getClassifier(@NotNull Name name) {
        return substitute(workerScope.getClassifier(name));
    }

    @Override
    public ClassDescriptor getObjectDescriptor(@NotNull Name name) {
        return substitute(workerScope.getObjectDescriptor(name));
    }

    @NotNull
    @Override
    public Set<ClassDescriptor> getObjectDescriptors() {
        return substitute(workerScope.getObjectDescriptors());
    }

    @NotNull
    @Override
    public Set<FunctionDescriptor> getFunctions(@NotNull Name name) {
        return substitute(workerScope.getFunctions(name));
    }

    @Override
    public NamespaceDescriptor getNamespace(@NotNull Name name) {
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
    public Collection<DeclarationDescriptor> getDeclarationsByLabel(LabelName labelName) {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    public PropertyDescriptor getPropertyByFieldReference(@NotNull Name fieldName) {
        throw new UnsupportedOperationException(); // TODO
    }

    @NotNull
    @Override
    public Collection<DeclarationDescriptor> getAllDescriptors() {
        if (allDescriptors == null) {
            allDescriptors = Sets.newHashSet();
            for (DeclarationDescriptor descriptor : workerScope.getAllDescriptors()) {
                DeclarationDescriptor substitute = substitute(descriptor);
//                assert substitute != null : descriptor;
                if (substitute != null) {
                    allDescriptors.add(substitute);
                }
            }
        }
        return allDescriptors;
    }
}
