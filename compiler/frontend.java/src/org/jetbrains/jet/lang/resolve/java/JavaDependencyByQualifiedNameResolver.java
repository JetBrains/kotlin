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

package org.jetbrains.jet.lang.resolve.java;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.ClassifierDescriptor;
import org.jetbrains.jet.lang.descriptors.PackageViewDescriptor;
import org.jetbrains.jet.lang.descriptors.SubModuleDescriptor;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.ChainedScope;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.types.DependencyClassByQualifiedNameResolver;

import java.util.Iterator;

public class JavaDependencyByQualifiedNameResolver implements DependencyClassByQualifiedNameResolver {

    private final JetScope rootScope;

    public JavaDependencyByQualifiedNameResolver(@NotNull SubModuleDescriptor subModule) {
        this.rootScope = DescriptorUtils.getRootPackage(subModule).getMemberScope();
    }

    @Nullable
    @Override
    public ClassDescriptor resolveClass(@NotNull FqName fqName) {
        assert !fqName.isRoot() : "A class can not have an empty fqName";

        if (fqName.parent().isRoot()) {
            return getClass(rootScope, fqName.shortName());
        }

        JetScope currentScope = rootScope;
        for (Iterator<Name> iterator = fqName.pathSegments().iterator(); iterator.hasNext(); ) {
            Name name = iterator.next();

            ClassDescriptor classDescriptor = getClass(currentScope, name);
            if (!iterator.hasNext()) return classDescriptor;

            PackageViewDescriptor packageView = currentScope.getPackage(name);


            if (packageView == null && classDescriptor == null) {
                return null;
            }
            if (packageView != null && classDescriptor != null) {
                currentScope = new ChainedScope(null, packageView.getMemberScope(), classDescriptor.getUnsubstitutedInnerClassesScope());
            }
            else if (packageView != null) {
                currentScope = packageView.getMemberScope();
            }
            else {
                currentScope = classDescriptor.getUnsubstitutedInnerClassesScope();
            }
        }

        return null;
    }

    @Nullable
    private static ClassDescriptor getClass(@NotNull JetScope scope, @NotNull Name name) {
        ClassifierDescriptor classifier = scope.getClassifier(name);
        if (classifier != null) {
            assert classifier instanceof ClassDescriptor : "Only classes should appear as classifiers in this context: " + classifier;
        }
        return (ClassDescriptor) classifier;
    }
}
