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

import com.google.common.collect.Sets;
import com.intellij.openapi.progress.ProgressIndicatorProvider;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiPackage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.resolve.java.*;
import org.jetbrains.jet.lang.resolve.name.FqName;

import java.util.Collection;

public final class ScopeUtils {
    private ScopeUtils() {
    }

    @NotNull
    public static Collection<DeclarationDescriptor> computeAllPackageDeclarations(
            PsiPackage psiPackage,
            JavaSemanticServices javaSemanticServices
    ) {
        Collection<DeclarationDescriptor> result = Sets.newHashSet();
        JavaDescriptorResolver descriptorResolver = javaSemanticServices.getDescriptorResolver();

        for (PsiPackage psiSubPackage : psiPackage.getSubPackages()) {
            NamespaceDescriptor childNs = descriptorResolver.resolveNamespace(
                    new FqName(psiSubPackage.getQualifiedName()), DescriptorSearchRule.IGNORE_IF_FOUND_IN_KOTLIN);
            if (childNs != null) {
                result.add(childNs);
            }
        }

        for (PsiClass psiClass : javaSemanticServices.getPsiClassFinder().findPsiClasses(psiPackage)) {
            if (PackageClassUtils.isPackageClass(psiClass)) {
                continue;
            }

            if (psiClass instanceof JetJavaMirrorMarker) {
                continue;
            }

            if (psiClass.hasModifierProperty(PsiModifier.PUBLIC)) {
                ProgressIndicatorProvider.checkCanceled();
                FqName fqName = new FqName(psiClass.getQualifiedName());
                ClassDescriptor classDescriptor = descriptorResolver.resolveClass(fqName, DescriptorSearchRule.IGNORE_IF_FOUND_IN_KOTLIN);
                if (classDescriptor != null) {
                    result.add(classDescriptor);
                }

                NamespaceDescriptor namespaceDescriptor = descriptorResolver.resolveNamespace(
                        fqName, DescriptorSearchRule.IGNORE_IF_FOUND_IN_KOTLIN);
                if (namespaceDescriptor != null) {
                    result.add(namespaceDescriptor);
                }
            }
        }
        return result;
    }

}
