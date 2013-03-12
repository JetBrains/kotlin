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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.ClassifierDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.resolve.java.DescriptorSearchRule;
import org.jetbrains.jet.lang.resolve.java.JavaSemanticServices;
import org.jetbrains.jet.lang.resolve.java.provider.PackagePsiDeclarationProvider;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;

import java.util.Collection;

import static org.jetbrains.jet.lang.resolve.java.scope.ScopeUtils.computeAllPackageDeclarations;

public abstract class JavaPackageScope extends JavaBaseScope {

    @NotNull
    private final FqName packageFQN;

    protected JavaPackageScope(
            @NotNull NamespaceDescriptor descriptor,
            @NotNull PackagePsiDeclarationProvider declarationProvider,
            @NotNull FqName packageFQN,
            @NotNull JavaSemanticServices semanticServices
    ) {
        super(descriptor, semanticServices, declarationProvider);
        this.packageFQN = packageFQN;
    }

    @Override
    public ClassifierDescriptor getClassifier(@NotNull Name name) {
        ClassDescriptor classDescriptor =
                getResolver().resolveClass(packageFQN.child(name), DescriptorSearchRule.IGNORE_IF_FOUND_IN_KOTLIN);
        if (classDescriptor == null || classDescriptor.getKind().isObject()) {
            return null;
        }
        return classDescriptor;
    }

    @Override
    public ClassDescriptor getObjectDescriptor(@NotNull Name name) {
        ClassDescriptor classDescriptor =
                getResolver().resolveClass(packageFQN.child(name), DescriptorSearchRule.IGNORE_IF_FOUND_IN_KOTLIN);
        if (classDescriptor != null && classDescriptor.getKind().isObject()) {
            return classDescriptor;
        }
        return null;
    }

    @Override
    public NamespaceDescriptor getNamespace(@NotNull Name name) {
        return getResolver().resolveNamespace(packageFQN.child(name), DescriptorSearchRule.INCLUDE_KOTLIN);
    }

    @NotNull
    @Override
    protected Collection<DeclarationDescriptor> computeAllDescriptors() {
        Collection<DeclarationDescriptor> result = super.computeAllDescriptors();
        result.addAll(computeAllPackageDeclarations(((PackagePsiDeclarationProvider) declarationProvider).getPsiPackage(), semanticServices));
        return result;
    }
}
