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
import org.jetbrains.annotations.Nullable;
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

    @NotNull
    private final FqName packageFQN;

    public JavaPackageScope(
            @NotNull FqName packageFQN,
            @NotNull JavaSemanticServices semanticServices,
            @NotNull JavaDescriptorResolver.ResolverScopeData resolverNamespaceData) {
        super(semanticServices, resolverNamespaceData);
        this.packageFQN = packageFQN;
    }

    @Override
    public ClassifierDescriptor getClassifier(@NotNull String name) {
        ClassDescriptor classDescriptor = semanticServices.getDescriptorResolver().resolveClass(packageFQN.child(name), DescriptorSearchRule.IGNORE_IF_FOUND_IN_KOTLIN);
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
        return semanticServices.getDescriptorResolver().resolveNamespace(packageFQN.child(name), DescriptorSearchRule.INCLUDE_KOTLIN);
    }
}
