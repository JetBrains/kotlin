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
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiPackage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.ClassifierDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;

import java.util.Collection;
import java.util.HashSet;

/**
 * @author abreslav
 */
public class JavaPackageScope extends JavaClassOrPackageScope {

    private static final Logger LOG = Logger.getInstance("#com.intellij.psi.impl.migration.PsiMigrationImpl");

    @NotNull
    private final String packageFQN;

    private Collection<DeclarationDescriptor> allDescriptors;

    public JavaPackageScope(
            @NotNull String packageFQN,
            @NotNull NamespaceDescriptor containingDescriptor,
            @NotNull JavaSemanticServices semanticServices) {
        super(containingDescriptor, semanticServices, getPiClass(packageFQN, semanticServices));
        this.packageFQN = packageFQN;
    }

    private static PsiClass getPiClass(String packageFQN, JavaSemanticServices semanticServices) {
        // TODO: move this check outside
        // If this package is actually a Kotlin namespace, then we access it through a namespace descriptor, and
        // Kotlin functions are already there
        NamespaceDescriptor kotlinNamespaceDescriptor = semanticServices.getKotlinNamespaceDescriptor(packageFQN);
        if (kotlinNamespaceDescriptor != null) {
            return null;
        } else {
            // TODO: what is GlobalSearchScope
            return semanticServices.getDescriptorResolver().javaFacade.findClass(getQualifiedName(packageFQN, JvmAbi.PACKAGE_CLASS));
        }
    }

    @Override
    public ClassifierDescriptor getClassifier(@NotNull String name) {
        return semanticServices.getDescriptorResolver().resolveClass(getQualifiedName(packageFQN, name));
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

    @NotNull
    @Override
    public Collection<DeclarationDescriptor> getAllDescriptors() {
        if (allDescriptors == null) {
            allDescriptors = Sets.newHashSet();

            final PsiPackage javaPackage = semanticServices.getDescriptorResolver().findPackage(packageFQN);

            if (javaPackage != null) {
                boolean isKotlinNamespace = semanticServices.getKotlinNamespaceDescriptor(javaPackage.getQualifiedName()) != null;
                final JavaDescriptorResolver descriptorResolver = semanticServices.getDescriptorResolver();

                for (PsiPackage psiSubPackage : javaPackage.getSubPackages()) {
                    if (semanticServices.getKotlinNamespaceDescriptor(psiSubPackage.getQualifiedName()) == null) {
                        allDescriptors.add(descriptorResolver.resolveNamespace(psiSubPackage.getQualifiedName()));
                    }
                }

                for (PsiClass psiClass : javaPackage.getClasses()) {
                    if (isKotlinNamespace && JvmAbi.PACKAGE_CLASS.equals(psiClass.getName())) {
                        continue;
                    }

                    // If this is a Kotlin class, we have already taken it through a containing namespace descriptor
                    ClassDescriptor kotlinClassDescriptor = semanticServices.getKotlinClassDescriptor(psiClass.getQualifiedName());
                    if (kotlinClassDescriptor != null) {
                        continue;
                    }

                    // TODO: Temp hack for collection function descriptors from java
                    if (JvmAbi.PACKAGE_CLASS.equals(psiClass.getName())) {
                        HashSet<String> methodNames = new HashSet<String>();
                        for (PsiMethod psiMethod : psiClass.getMethods()) {
                            methodNames.add(psiMethod.getName());
                        }

                        for (String methodName : methodNames) {
                            try {
                                allDescriptors.addAll(getFunctions(methodName));
                            } catch (ProcessCanceledException cancelException) {
                                throw cancelException;
                            } catch (RuntimeException ex) {
                                LOG.error(ex);
                            }
                        }
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

    private static String getQualifiedName(@NotNull String packageFQN, @NotNull String name) {
        return (packageFQN.isEmpty() ? "" : packageFQN + ".") + name;
    }
}
