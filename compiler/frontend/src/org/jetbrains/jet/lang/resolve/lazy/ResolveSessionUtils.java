/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.resolve.lazy;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.JetNamed;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.name.NamePackage;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class ResolveSessionUtils {

    // This name is used as a key for the case when something has no name _due to a syntactic error_
    // Example: fun (x: Int) = 5
    //          There's no name for this function in the PSI
    // The name contains a GUID to avoid clashes, if a clash happens, it's not a big deal: the code does not compile anyway
    public static final Name NO_NAME_FOR_LAZY_RESOLVE = Name.identifier("no_name_in_PSI_for_lazy_resolve_3d19d79d_1ba9_4cd0_b7f5_b46aa3cd5d40");

    public static final Predicate<ClassDescriptor> NON_SINGLETON_FILTER = new Predicate<ClassDescriptor>() {
        @Override
        public boolean apply(@Nullable ClassDescriptor descriptor) {
            assert descriptor != null;
            return !descriptor.getKind().isSingleton();
        }
    };

    public static final Predicate<ClassDescriptor> SINGLETON_FILTER = new Predicate<ClassDescriptor>() {
        @Override
        public boolean apply(@Nullable ClassDescriptor descriptor) {
            assert descriptor != null;
            return descriptor.getKind().isSingleton();
        }
    };

    private ResolveSessionUtils() {
    }

    @NotNull
    public static Collection<ClassDescriptor> getClassDescriptorsByFqName(@NotNull KotlinCodeAnalyzer analyzer, @NotNull FqName fqName) {
        return getClassOrObjectDescriptorsByFqName(analyzer, fqName, NON_SINGLETON_FILTER);
    }

    @NotNull
    public static Collection<ClassDescriptor> getClassOrObjectDescriptorsByFqName(
            @NotNull KotlinCodeAnalyzer analyzer,
            @NotNull FqName fqName,
            @NotNull Predicate<ClassDescriptor> filter
    ) {
        if (fqName.isRoot()) return Collections.emptyList();

        Collection<ClassDescriptor> classDescriptors = Lists.newArrayList();

        FqName packageFqName = fqName.parent();
        while (true) {
            PackageViewDescriptor packageDescriptor = analyzer.getModuleDescriptor().getPackage(packageFqName);
            if (packageDescriptor != null) {
                FqName classInPackagePath = NamePackage.tail(fqName, packageFqName);
                ClassDescriptor classDescriptor = findByQualifiedName(packageDescriptor.getMemberScope(), classInPackagePath, filter);
                if (classDescriptor != null) {
                    classDescriptors.add(classDescriptor);
                }
            }

            if (packageFqName.isRoot()) {
                break;
            }
            else {
                packageFqName = packageFqName.parent();
            }
        }

        return classDescriptors;
    }

    @Nullable
    public static ClassDescriptor findByQualifiedName(@NotNull JetScope packageScope, @NotNull FqName path) {
        return findByQualifiedName(packageScope, path, Predicates.<ClassDescriptor>alwaysTrue());
    }

    @Nullable
    private static ClassDescriptor findByQualifiedName(
            @NotNull JetScope jetScope,
            @NotNull FqName path,
            @NotNull Predicate<ClassDescriptor> filter
    ) {
        if (path.isRoot()) return null;

        if (NamePackage.isOneSegmentFQN(path)) {
            Name shortName = path.shortName();
            ClassifierDescriptor classifier = jetScope.getClassifier(shortName);
            if (classifier instanceof ClassDescriptor) {
                ClassDescriptor resultDescriptor = (ClassDescriptor) classifier;

                if (filter.apply(resultDescriptor)) {
                    return resultDescriptor;
                }
            }

            return null;
        }

        Name firstName = NamePackage.getFirstSegment(path);

        // Search in internal class
        ClassifierDescriptor classifier = jetScope.getClassifier(firstName);
        if (classifier instanceof ClassDescriptor) {
            return findByQualifiedName(
                    ((ClassDescriptor) classifier).getUnsubstitutedInnerClassesScope(),
                    NamePackage.withoutFirstSegment(path),
                    filter);
        }

        // TODO: search in class object

        return null;
    }

    @NotNull
    public static Name safeNameForLazyResolve(@NotNull JetNamed named) {
        return safeNameForLazyResolve(named.getNameAsName());
    }

    @NotNull
    public static Name safeNameForLazyResolve(@Nullable Name name) {
        return name != null ? name : NO_NAME_FOR_LAZY_RESOLVE;
    }
}
