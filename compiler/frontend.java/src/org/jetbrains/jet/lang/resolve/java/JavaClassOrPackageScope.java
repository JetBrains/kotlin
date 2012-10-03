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
import com.intellij.openapi.progress.ProgressIndicatorProvider;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiPackage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.resolve.java.JavaDescriptorResolveData.ResolverScopeData;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScopeImpl;

import java.util.Collection;

/**
 * @author Stepan Koltsov
 */
public abstract class JavaClassOrPackageScope extends JetScopeImpl {

    @NotNull
    protected final JavaSemanticServices semanticServices;
    @NotNull
    protected final ResolverScopeData resolverScopeData;

    // cache
    private Collection<DeclarationDescriptor> allDescriptors;

    protected JavaClassOrPackageScope(
            @NotNull JavaSemanticServices semanticServices,
            @NotNull ResolverScopeData resolverScopeData) {
        this.semanticServices = semanticServices;
        this.resolverScopeData = resolverScopeData;
    }

    @NotNull
    @Override
    public DeclarationDescriptor getContainingDeclaration() {
        return resolverScopeData.getClassOrNamespaceDescriptor();
    }

    @NotNull
    @Override
    public Collection<VariableDescriptor> getProperties(@NotNull Name name) {
        return semanticServices.getDescriptorResolver().resolveFieldGroupByName(name, resolverScopeData);
    }

    @NotNull
    @Override
    public Collection<FunctionDescriptor> getFunctions(@NotNull Name name) {
        return semanticServices.getDescriptorResolver().resolveFunctionGroup(name, resolverScopeData);
    }

    @NotNull
    @Override
    public Collection<DeclarationDescriptor> getAllDescriptors() {
        if (allDescriptors == null) {
            allDescriptors = Sets.newHashSet();

            if (resolverScopeData.getPsiClass() != null) {

                ProgressIndicatorProvider.checkCanceled();
                allDescriptors.addAll(semanticServices.getDescriptorResolver().resolveMethods(resolverScopeData));

                ProgressIndicatorProvider.checkCanceled();
                allDescriptors.addAll(semanticServices.getDescriptorResolver().resolveFieldGroup(resolverScopeData));

                // TODO: Trying to hack the situation when we produce namespace descriptor for java class and still want to see inner classes
                if (getContainingDeclaration() instanceof JavaNamespaceDescriptor) {
                    allDescriptors.addAll(semanticServices.getDescriptorResolver().resolveInnerClasses(
                            resolverScopeData.getClassOrNamespaceDescriptor(), resolverScopeData.getPsiClass(), false));
                }
                else {
                    allDescriptors.addAll(semanticServices.getDescriptorResolver().resolveInnerClasses(
                            resolverScopeData.getClassOrNamespaceDescriptor(), resolverScopeData.getPsiClass(),
                            resolverScopeData.isStaticMembers()));
                }
            }

            if (resolverScopeData.getPsiPackage() != null) {
                FqName packageFqName = resolverScopeData.getFqName();
                boolean isKotlinNamespace = packageFqName != null && semanticServices.getKotlinNamespaceDescriptor(packageFqName) != null;
                final JavaDescriptorResolver descriptorResolver = semanticServices.getDescriptorResolver();

                for (PsiPackage psiSubPackage : resolverScopeData.getPsiPackage().getSubPackages()) {
                    NamespaceDescriptor childNs = descriptorResolver.resolveNamespace(
                            new FqName(psiSubPackage.getQualifiedName()), DescriptorSearchRule.IGNORE_IF_FOUND_IN_KOTLIN);
                    if (childNs != null) {
                        allDescriptors.add(childNs);
                    }
                }

                for (PsiClass psiClass : resolverScopeData.getPsiPackage().getClasses()) {
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
                        ProgressIndicatorProvider.checkCanceled();
                        ClassDescriptor classDescriptor = descriptorResolver
                                .resolveClass(new FqName(psiClass.getQualifiedName()), DescriptorSearchRule.IGNORE_IF_FOUND_IN_KOTLIN);
                        if (classDescriptor != null) {
                            allDescriptors.add(classDescriptor);
                        }
                    }
                }
            }
        }

        return allDescriptors;
    }
}
