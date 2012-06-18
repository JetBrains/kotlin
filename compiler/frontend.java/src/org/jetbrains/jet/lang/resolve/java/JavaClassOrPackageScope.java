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

package org.jetbrains.jet.lang.resolve.java;

import com.google.common.collect.Sets;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiPackage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.name.NamePredicate;
import org.jetbrains.jet.lang.resolve.scopes.DescriptorPredicate;
import org.jetbrains.jet.lang.resolve.scopes.DescriptorPredicateUtils;
import org.jetbrains.jet.lang.resolve.scopes.JetScopeImpl;

import java.util.Collection;

/**
 * @author Stepan Koltsov
 */
public abstract class JavaClassOrPackageScope extends JetScopeImpl {

    @NotNull
    protected final JavaSemanticServices semanticServices;
    @NotNull
    protected final JavaDescriptorResolver.ResolverScopeData resolverScopeData;

    // cache
    private Collection<DeclarationDescriptor> allDescriptors;

    protected JavaClassOrPackageScope(
            @NotNull JavaSemanticServices semanticServices,
            @NotNull JavaDescriptorResolver.ResolverScopeData resolverScopeData) {
        this.semanticServices = semanticServices;
        this.resolverScopeData = resolverScopeData;
    }

    @NotNull
    @Override
    public DeclarationDescriptor getContainingDeclaration() {
        return resolverScopeData.classOrNamespaceDescriptor;
    }

    @NotNull
    @Override
    public Collection<VariableDescriptor> getProperties(@NotNull Name name) {
        return semanticServices.getDescriptorResolver().resolveFieldGroup(resolverScopeData, NamePredicate.exact(name));
    }

    @NotNull
    @Override
    public Collection<FunctionDescriptor> getFunctions(@NotNull Name name) {
        return semanticServices.getDescriptorResolver().resolveMethods(resolverScopeData, NamePredicate.exact(name));
    }

    @NotNull
    @Override
    public Collection<DeclarationDescriptor> getAllDescriptors(@NotNull DescriptorPredicate predicate) {
        if (allDescriptors == null) {
            if (!predicate.includeAll()) {
                return computeAllDescriptors(predicate);
            }

            allDescriptors = computeAllDescriptors(DescriptorPredicate.all());
        }

        return DescriptorPredicateUtils.filter(allDescriptors, predicate);
    }

    private Collection<DeclarationDescriptor> computeAllDescriptors(@NotNull DescriptorPredicate predicate) {
        Collection<DeclarationDescriptor> allDescriptors = Sets.newHashSet();

        if (resolverScopeData.psiClass != null) {
            if (predicate.includeKind(DescriptorPredicate.DescriptorKind.CALLABLE_MEMBER)) {
                // TODO: filter by extension here
                allDescriptors.addAll(semanticServices.getDescriptorResolver().resolveMethods(resolverScopeData, predicate.asNamePredicate()));

                allDescriptors.addAll(semanticServices.getDescriptorResolver().resolveFieldGroup(resolverScopeData, predicate.asNamePredicate()));
            }

            if (predicate.includeKind(DescriptorPredicate.DescriptorKind.CLASS)) {
                // TODO: Trying to hack the situation when we produce namespace descriptor for java class and still want to see inner classes
                if (getContainingDeclaration() instanceof JavaNamespaceDescriptor) {
                    allDescriptors.addAll(semanticServices.getDescriptorResolver().resolveInnerClasses(
                            resolverScopeData.classOrNamespaceDescriptor, resolverScopeData.psiClass, false));
                }
                else  {
                    allDescriptors.addAll(semanticServices.getDescriptorResolver().resolveInnerClasses(
                            resolverScopeData.classOrNamespaceDescriptor, resolverScopeData.psiClass,
                            resolverScopeData.staticMembers));
                }
            }
        }

        if (resolverScopeData.psiPackage != null) {
            boolean isKotlinNamespace = semanticServices.getKotlinNamespaceDescriptor(resolverScopeData.fqName) != null;
            final JavaDescriptorResolver descriptorResolver = semanticServices.getDescriptorResolver();

            if (predicate.includeKind(DescriptorPredicate.DescriptorKind.NAMESPACE)) {
                for (PsiPackage psiSubPackage : resolverScopeData.psiPackage.getSubPackages()) {
                    NamespaceDescriptor childNs = descriptorResolver.resolveNamespace(
                            new FqName(psiSubPackage.getQualifiedName()), DescriptorSearchRule.IGNORE_IF_FOUND_IN_KOTLIN);
                    if (childNs != null) {
                        allDescriptors.add(childNs);
                    }
                }
            }

            if (predicate.includeKind(DescriptorPredicate.DescriptorKind.CLASS)) {
                for (PsiClass psiClass : resolverScopeData.psiPackage.getClasses()) {
                    if (isKotlinNamespace && JvmAbi.PACKAGE_CLASS.equals(psiClass.getName())) {
                        continue;
                    }

                    if (psiClass instanceof JetJavaMirrorMarker) {
                        continue;
                    }

                    // TODO: Temp hack for collection function descriptors from java
                    if (JvmAbi.PACKAGE_CLASS.equals(psiClass.getName())) {
                        continue;
                    }

                    if (psiClass.hasModifierProperty(PsiModifier.PUBLIC)) {
                        ClassDescriptor classDescriptor = descriptorResolver
                                .resolveClass(new FqName(psiClass.getQualifiedName()), DescriptorSearchRule.IGNORE_IF_FOUND_IN_KOTLIN);
                        if (classDescriptor != null) {
                            allDescriptors.add(classDescriptor);
                        }
                    }
                }
            }
        }

        // filter again, because previously filter wasn't accurate
        return DescriptorPredicateUtils.filter(allDescriptors, predicate);
    }
}
