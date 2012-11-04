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

package org.jetbrains.jet.lang.resolve.java.scope;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.intellij.openapi.progress.ProgressIndicatorProvider;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiPackage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.java.JavaSemanticServices;
import org.jetbrains.jet.lang.resolve.java.NamedMembers;
import org.jetbrains.jet.lang.resolve.java.data.ResolverScopeData;
import org.jetbrains.jet.lang.resolve.java.descriptor.JavaNamespaceDescriptor;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScopeImpl;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static org.jetbrains.jet.lang.resolve.java.scope.ScopeUtils.computeAllPackageDeclarations;

public abstract class JavaBaseScope extends JetScopeImpl {

    @NotNull
    protected final JavaSemanticServices semanticServices;

    @NotNull
    protected final ResolverScopeData resolverScopeData;

    @NotNull
    private final Map<Name, Set<FunctionDescriptor>> functionDescriptors = Maps.newHashMap();

    @NotNull
    private final Map<Name, Set<VariableDescriptor>> propertyDescriptors = Maps.newHashMap();
    @Nullable
    private Collection<DeclarationDescriptor> allDescriptors = null;
    @NotNull
    private final ClassOrNamespaceDescriptor descriptor;

    protected JavaBaseScope(
            @NotNull ClassOrNamespaceDescriptor descriptor,
            @NotNull JavaSemanticServices semanticServices,
            @NotNull ResolverScopeData resolverScopeData
    ) {
        this.semanticServices = semanticServices;
        this.resolverScopeData = resolverScopeData;
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
        return semanticServices.getDescriptorResolver().resolveFieldGroupByName(name, resolverScopeData, descriptor);
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
    private Set<FunctionDescriptor> computeFunctionDescriptor(@NotNull Name name) {
        return semanticServices.getDescriptorResolver().resolveFunctionGroup(name, resolverScopeData, descriptor);
    }

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
    private Collection<DeclarationDescriptor> computeAllDescriptors() {
        Collection<DeclarationDescriptor> result = Sets.newHashSet();
        if (!resolverScopeData.isEmpty()) {
            PsiClass psiClass = resolverScopeData.getPsiClass();
            computeFieldAndFunctionDescriptors(result);
            computeInnerClasses(psiClass, result);
        }

        PsiPackage psiPackage = resolverScopeData.getPsiPackage();
        if (psiPackage != null) {
            assert descriptor instanceof NamespaceDescriptor;
            result.addAll(computeAllPackageDeclarations(psiPackage, semanticServices, DescriptorUtils.getFQName(descriptor).toSafe()));
        }
        return result;
    }

    private void computeFieldAndFunctionDescriptors(Collection<DeclarationDescriptor> result) {
        for (NamedMembers members : resolverScopeData.getMembersCache().allMembers()) {
            Name name = members.getName();
            ProgressIndicatorProvider.checkCanceled();
            result.addAll(getFunctions(name));
            ProgressIndicatorProvider.checkCanceled();
            result.addAll(getProperties(name));
        }
    }

    private void computeInnerClasses(
            @NotNull PsiClass psiClass,
            @NotNull Collection<DeclarationDescriptor> result
    ) {
        // TODO: Trying to hack the situation when we produce namespace descriptor for java class and still want to see inner classes
        if (descriptor instanceof JavaNamespaceDescriptor) {
            result.addAll(semanticServices.getDescriptorResolver().resolveInnerClasses(descriptor, psiClass, false));
        }
        else {
            result.addAll(semanticServices.getDescriptorResolver().resolveInnerClasses(
                    descriptor, psiClass, resolverScopeData.isStaticMembers()));
        }
    }

    @NotNull
    public ResolverScopeData getResolverScopeData() {
        return resolverScopeData;
    }
}
