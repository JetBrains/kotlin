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
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiPackage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.ClassifierDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.FqName;

import java.util.Collection;

/**
 * @author abreslav
 */
public class JavaPackageScope extends JavaClassOrPackageScope {

    private static final Logger LOG = Logger.getInstance("#com.intellij.psi.impl.migration.PsiMigrationImpl");

    @NotNull
    private final FqName packageFQN;

    private Collection<DeclarationDescriptor> allDescriptors;

    public JavaPackageScope(
            @NotNull FqName packageFQN,
            @NotNull NamespaceDescriptor containingDescriptor,
            @NotNull JavaSemanticServices semanticServices) {
        super(containingDescriptor, semanticServices, getPiClass(packageFQN, semanticServices));
        this.packageFQN = packageFQN;
    }

    private static PsiClass getPiClass(FqName packageFQN, JavaSemanticServices semanticServices) {
        // TODO: move this check outside
        // If this package is actually a Kotlin namespace, then we access it through a namespace descriptor, and
        // Kotlin functions are already there
        NamespaceDescriptor kotlinNamespaceDescriptor = semanticServices.getKotlinNamespaceDescriptor(packageFQN);
        if (kotlinNamespaceDescriptor != null) {
            return null;
        } else {
            // TODO: what is GlobalSearchScope
            return semanticServices.getDescriptorResolver().javaFacade.findClass(getQualifiedName(packageFQN, JvmAbi.PACKAGE_CLASS).getFqName());
        }
    }

    @Override
    public ClassifierDescriptor getClassifier(@NotNull String name) {
        ClassDescriptor classDescriptor = semanticServices.getDescriptorResolver().resolveClass(getQualifiedName(packageFQN, name));
        if (classDescriptor == null || DescriptorUtils.isObject(classDescriptor)) {
            // TODO: this is a big hack against several things that I barely understand myself and cannot explain
            // 1. We should not return objects from this method, and maybe JDR.resolveClass should not return too
            // 2. JDR should not return classes being analyzed
            return null;
        }
        return classDescriptor;
    }

    @Override
    public ClassDescriptor getObjectDescriptor(@NotNull String name) {
        // TODO
        return null;
    }

    @Override
    public NamespaceDescriptor getNamespace(@NotNull String name) {
        return semanticServices.getDescriptorResolver().resolveNamespace(getQualifiedName(packageFQN, name));
    }

    @Override
    protected boolean staticMembers() {
        return true;
    }

    /**
     * @see JavaClassMembersScope#getAllDescriptors()
     */
    @NotNull
    @Override
    public Collection<DeclarationDescriptor> getAllDescriptors() {
        if (allDescriptors == null) {
            allDescriptors = Sets.newHashSet();

            if (psiClass != null) {
                allDescriptors.addAll(semanticServices.getDescriptorResolver().resolveMethods(psiClass, descriptor));

                allDescriptors.addAll(semanticServices.getDescriptorResolver().resolveFieldGroup(descriptor, psiClass, staticMembers()));
            }

            final PsiPackage javaPackage = semanticServices.getDescriptorResolver().findPackage(packageFQN);

            if (javaPackage != null) {
                boolean isKotlinNamespace = semanticServices.getKotlinNamespaceDescriptor(new FqName(javaPackage.getQualifiedName())) != null;
                final JavaDescriptorResolver descriptorResolver = semanticServices.getDescriptorResolver();

                for (PsiPackage psiSubPackage : javaPackage.getSubPackages()) {
                    if (semanticServices.getKotlinNamespaceDescriptor(new FqName(psiSubPackage.getQualifiedName())) == null) {
                        allDescriptors.add(descriptorResolver.resolveNamespace(new FqName(psiSubPackage.getQualifiedName())));
                    }
                }

                for (PsiClass psiClass : javaPackage.getClasses()) {
                    if (isKotlinNamespace && JvmAbi.PACKAGE_CLASS.equals(psiClass.getName())) {
                        continue;
                    }

                    // If this is a Kotlin class, we have already taken it through a containing namespace descriptor
                    ClassDescriptor kotlinClassDescriptor = semanticServices.getKotlinClassDescriptor(new FqName(psiClass.getQualifiedName()));
                    if (kotlinClassDescriptor != null) {
                        continue;
                    }

                    // TODO: Temp hack for collection function descriptors from java
                    if (JvmAbi.PACKAGE_CLASS.equals(psiClass.getName())) {
                        continue;
                    }

                    if (psiClass.hasModifierProperty(PsiModifier.PUBLIC)) {
                        ClassDescriptor classDescriptor = descriptorResolver.resolveClass(psiClass);
                        if (classDescriptor != null) {
                            allDescriptors.add(classDescriptor);
                        }
                    }
                }
            }
        }

        return allDescriptors;
    }

    @NotNull
    private static FqName getQualifiedName(@NotNull FqName packageFQN, @NotNull String name) {
        return packageFQN.child(name);
    }
}
