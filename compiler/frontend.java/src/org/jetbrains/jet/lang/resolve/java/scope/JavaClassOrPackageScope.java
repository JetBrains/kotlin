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

import com.google.common.collect.Sets;
import com.intellij.openapi.progress.ProgressIndicatorProvider;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiPackage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.resolve.java.*;
import org.jetbrains.jet.lang.resolve.java.data.ResolverScopeData;
import org.jetbrains.jet.lang.resolve.java.descriptor.JavaNamespaceDescriptor;
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
            @NotNull ResolverScopeData resolverScopeData
    ) {
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
        if (allDescriptors != null) {
            return allDescriptors;
        }

        allDescriptors = Sets.newHashSet();

        PsiClass psiClass = resolverScopeData.getPsiClass();
        if (psiClass != null) {
            allDescriptors.addAll(computeAllClassDeclarations(psiClass, semanticServices, resolverScopeData, getContainingDeclaration()));
        }

        PsiPackage psiPackage = resolverScopeData.getPsiPackage();
        if (psiPackage != null) {
            allDescriptors.addAll(computeAllPackageDeclarations(psiPackage, semanticServices, resolverScopeData.getFqName()));
        }

        return allDescriptors;
    }

    @NotNull
    private static Collection<DeclarationDescriptor> computeAllPackageDeclarations(
            PsiPackage psiPackage,
            JavaSemanticServices javaSemanticServices,
            FqName packageFqName
    ) {
        Collection<DeclarationDescriptor> result = Sets.newHashSet();
        boolean isKotlinNamespace = packageFqName != null && javaSemanticServices.getKotlinNamespaceDescriptor(packageFqName) != null;
        final JavaDescriptorResolver descriptorResolver = javaSemanticServices.getDescriptorResolver();

        for (PsiPackage psiSubPackage : psiPackage.getSubPackages()) {
            NamespaceDescriptor childNs = descriptorResolver.resolveNamespace(
                    new FqName(psiSubPackage.getQualifiedName()), DescriptorSearchRule.IGNORE_IF_FOUND_IN_KOTLIN);
            if (childNs != null) {
                result.add(childNs);
            }
        }

        for (PsiClass psiClass : psiPackage.getClasses()) {
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
                    result.add(classDescriptor);
                }
            }
        }
        return result;
    }

    @NotNull
    private static Collection<DeclarationDescriptor> computeAllClassDeclarations(
            @NotNull PsiClass psiClass,
            @NotNull JavaSemanticServices javaSemanticServices,
            @NotNull ResolverScopeData scopeData,
            @NotNull DeclarationDescriptor containingDeclaration
    ) {
        Collection<DeclarationDescriptor> result = Sets.newHashSet();
        ProgressIndicatorProvider.checkCanceled();
        result.addAll(javaSemanticServices.getDescriptorResolver().resolveMethods(scopeData));

        ProgressIndicatorProvider.checkCanceled();
        result.addAll(javaSemanticServices.getDescriptorResolver().resolveFieldGroup(scopeData));

        // TODO: Trying to hack the situation when we produce namespace descriptor for java class and still want to see inner classes
        if (containingDeclaration instanceof JavaNamespaceDescriptor) {
            result.addAll(javaSemanticServices.getDescriptorResolver().resolveInnerClasses(
                    scopeData.getClassOrNamespaceDescriptor(), psiClass, false));
        }
        else {
            result.addAll(javaSemanticServices.getDescriptorResolver().resolveInnerClasses(
                    scopeData.getClassOrNamespaceDescriptor(), psiClass,
                    scopeData.isStaticMembers()));
        }
        return result;
    }
}
