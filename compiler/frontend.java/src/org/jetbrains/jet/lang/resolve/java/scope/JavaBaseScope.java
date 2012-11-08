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
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiPackage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.java.JavaDescriptorResolver;
import org.jetbrains.jet.lang.resolve.java.JavaSemanticServices;
import org.jetbrains.jet.lang.resolve.java.descriptor.JavaNamespaceDescriptor;
import org.jetbrains.jet.lang.resolve.java.provider.*;
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
    protected final PsiDeclarationProvider declarationProvider;

    @NotNull
    private final Map<Name, Set<FunctionDescriptor>> functionDescriptors = Maps.newHashMap();

    @NotNull
    private final Map<Name, Set<VariableDescriptor>> propertyDescriptors = Maps.newHashMap();
    @Nullable
    private Collection<DeclarationDescriptor> allDescriptors = null;
    @NotNull
    protected final ClassOrNamespaceDescriptor descriptor;

    protected JavaBaseScope(
            @NotNull ClassOrNamespaceDescriptor descriptor,
            @NotNull JavaSemanticServices semanticServices,
            @NotNull PsiDeclarationProvider declarationProvider
    ) {
        this.semanticServices = semanticServices;
        this.declarationProvider = declarationProvider;
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
        return getResolver().resolveFieldGroupByName(name, declarationProvider, descriptor);
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
    private Collection<DeclarationDescriptor> computeAllDescriptors() {
        Collection<DeclarationDescriptor> result = Sets.newHashSet();
        if (declarationProvider instanceof ClassPsiDeclarationProvider) {
            PsiClass psiClass = ((ClassPsiDeclarationProvider) declarationProvider).getPsiClass();
            computeFieldAndFunctionDescriptors(result);
            computeInnerClasses(psiClass, result);
        }
        if (declarationProvider instanceof PackagePsiDeclarationProvider) {
            PsiPackage psiPackage = ((PackagePsiDeclarationProvider) declarationProvider).getPsiPackage();
            assert descriptor instanceof NamespaceDescriptor;
            result.addAll(computeAllPackageDeclarations(psiPackage, semanticServices, DescriptorUtils.getFQName(descriptor).toSafe()));
        }
        return result;
    }

    private void computeFieldAndFunctionDescriptors(Collection<DeclarationDescriptor> result) {
        for (NamedMembers members : declarationProvider.getMembersCache().allMembers()) {
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
            result.addAll(getResolver().resolveInnerClasses(descriptor, psiClass, false));
        }
        else {
            result.addAll(getResolver().resolveInnerClasses(
                    descriptor, psiClass, ((ClassPsiDeclarationProviderImpl) declarationProvider).isStaticMembers()));
        }
    }

    @NotNull
    protected JavaDescriptorResolver getResolver() {
        return semanticServices.getDescriptorResolver();
    }

    //TODO: remove this method
    @NotNull
    public PsiElement getPsiElement() {
        if (declarationProvider instanceof ClassPsiDeclarationProvider) {
            return ((ClassPsiDeclarationProvider) declarationProvider).getPsiClass();
        }
        if (declarationProvider instanceof PackagePsiDeclarationProvider) {
            return ((PackagePsiDeclarationProvider) declarationProvider).getPsiPackage();
        }
        throw new IllegalStateException();
    }
}
