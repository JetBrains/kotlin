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

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.ClassOrNamespaceDescriptor;
import org.jetbrains.jet.lang.descriptors.ClassifierDescriptor;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.resolve.java.DescriptorSearchRule;
import org.jetbrains.jet.lang.resolve.java.JavaSemanticServices;
import org.jetbrains.jet.lang.resolve.java.provider.ClassPsiDeclarationProvider;
import org.jetbrains.jet.lang.resolve.java.provider.PackagePsiDeclarationProvider;
import org.jetbrains.jet.lang.resolve.java.provider.PsiDeclarationProvider;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;

/**
 * @author abreslav
 */
public class JavaPackageScope extends JavaBaseScope {

    @NotNull
    private final FqName packageFQN;
    @NotNull
    private final PsiDeclarationProvider declarationProvider;

    public JavaPackageScope(
            @NotNull ClassOrNamespaceDescriptor descriptor,
            @NotNull FqName packageFQN,
            @NotNull JavaSemanticServices semanticServices,
            @NotNull PsiDeclarationProvider declarationProvider) {
        super(descriptor, semanticServices, declarationProvider);
        this.declarationProvider = declarationProvider;
        this.packageFQN = packageFQN;
    }

    @Override
    public ClassifierDescriptor getClassifier(@NotNull Name name) {
        return semanticServices.getDescriptorResolver().resolveClass(packageFQN.child(name), DescriptorSearchRule.IGNORE_IF_FOUND_IN_KOTLIN);
    }

    @Override
    public ClassDescriptor getObjectDescriptor(@NotNull Name name) {
        //TODO: check that class is an object
        return semanticServices.getDescriptorResolver().resolveClass(packageFQN.child(name), DescriptorSearchRule.IGNORE_IF_FOUND_IN_KOTLIN);
    }

    @Override
    public NamespaceDescriptor getNamespace(@NotNull Name name) {
        return semanticServices.getDescriptorResolver().resolveNamespace(packageFQN.child(name), DescriptorSearchRule.INCLUDE_KOTLIN);
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
