/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.resolve.java.scope;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.intellij.openapi.progress.ProgressIndicatorProvider;
import com.intellij.openapi.util.Condition;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.resolve.java.JavaDescriptorResolver;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScopeImpl;

import java.util.*;

public abstract class JavaBaseScope extends JetScopeImpl {
    @NotNull
    protected final JavaDescriptorResolver javaDescriptorResolver;
    @NotNull
    protected final MembersProvider membersProvider;
    @NotNull
    private final Map<Name, Set<FunctionDescriptor>> functionDescriptors = Maps.newHashMap();
    @NotNull
    private final Map<Name, Set<VariableDescriptor>> propertyDescriptors = Maps.newHashMap();
    @Nullable
    private Collection<DeclarationDescriptor> allDescriptors = null;
    @Nullable
    private Set<ClassDescriptor> objectDescriptors = null;
    @NotNull
    protected final ClassOrNamespaceDescriptor descriptor;

    private Collection<ClassDescriptor> innerClasses = null;


    protected JavaBaseScope(
            @NotNull ClassOrNamespaceDescriptor descriptor,
            @NotNull JavaDescriptorResolver javaDescriptorResolver,
            @NotNull MembersProvider membersProvider
    ) {
        this.javaDescriptorResolver = javaDescriptorResolver;
        this.membersProvider = membersProvider;
        this.descriptor = descriptor;
    }

    @NotNull
    @Override
    public DeclarationDescriptor getContainingDeclaration() {
        return descriptor;
    }

    @NotNull
    @Override
    public Collection<VariableDescriptor> getProperties(@NotNull Name name) {
        Set<VariableDescriptor> cached = propertyDescriptors.get(name);
        if (cached != null) return cached;

        if (allDescriptorsComputed()) {
            return Collections.emptySet();
        }

        Set<VariableDescriptor> computedDescriptors = computePropertyDescriptors(name);
        propertyDescriptors.put(name, computedDescriptors);
        return computedDescriptors;
    }

    @NotNull
    private Set<VariableDescriptor> computePropertyDescriptors(@NotNull Name name) {
        NamedMembers members = membersProvider.get(name);
        if (members == null) {
            return Collections.emptySet();
        }
        return javaDescriptorResolver.resolveFieldGroup(members, descriptor);
    }

    @NotNull
    @Override
    public Collection<FunctionDescriptor> getFunctions(@NotNull Name name) {
        Set<FunctionDescriptor> cached = functionDescriptors.get(name);
        if (cached != null) return cached;

        if (allDescriptorsComputed()) {
            return Collections.emptySet();
        }

        Set<FunctionDescriptor> computedDescriptors = computeFunctionDescriptor(name);
        functionDescriptors.put(name, computedDescriptors);
        return computedDescriptors;
    }

    @NotNull
    protected abstract Set<FunctionDescriptor> computeFunctionDescriptor(@NotNull Name name);

    @NotNull
    @Override
    public Collection<DeclarationDescriptor> getAllDescriptors() {
        if (allDescriptorsComputed()) {
            return allDescriptors;
        }

        allDescriptors = computeAllDescriptors();

        return allDescriptors;
    }

    private boolean allDescriptorsComputed() {
        return allDescriptors != null;
    }

    @NotNull
    protected Collection<DeclarationDescriptor> computeAllDescriptors() {
        Collection<DeclarationDescriptor> result = Sets.newHashSet();
        result.addAll(computeFieldAndFunctionDescriptors());
        result.addAll(filterObjects(getInnerClasses(), false));
        return result;
    }

    @NotNull
    @Override
    public Set<ClassDescriptor> getObjectDescriptors() {
        if (objectDescriptors == null) {
            objectDescriptors = new HashSet<ClassDescriptor>(filterObjects(getInnerClasses(), true));
        }
        return objectDescriptors;
    }

    @NotNull
    protected abstract Collection<ClassDescriptor> computeInnerClasses();

    @NotNull
    private Collection<DeclarationDescriptor> computeFieldAndFunctionDescriptors() {
        Collection<DeclarationDescriptor> result = Lists.newArrayList();
        for (NamedMembers members : membersProvider.allMembers()) {
            Name name = members.getName();
            ProgressIndicatorProvider.checkCanceled();
            result.addAll(getFunctions(name));
            ProgressIndicatorProvider.checkCanceled();
            result.addAll(getProperties(name));
        }
        return result;
    }

    @NotNull
    protected Collection<ClassDescriptor> getInnerClasses() {
        if (innerClasses == null) {
            innerClasses = computeInnerClasses();
        }
        return innerClasses;
    }

    private static <T extends ClassDescriptor> Collection<T> filterObjects(Collection<T> classes, final boolean objects) {
        return ContainerUtil.filter(classes, new Condition<T>() {
            @Override
            public boolean value(T classDescriptor) {
                return classDescriptor.getKind().isObject() == objects;
            }
        });
    }
}
