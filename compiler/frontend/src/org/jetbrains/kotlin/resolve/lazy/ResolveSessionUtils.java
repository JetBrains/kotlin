/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.resolve.lazy;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.ClassDescriptor;
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor;
import org.jetbrains.kotlin.descriptors.ModuleDescriptor;
import org.jetbrains.kotlin.descriptors.PackageViewDescriptor;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.name.NamePackage;
import org.jetbrains.kotlin.name.SpecialNames;
import org.jetbrains.kotlin.psi.JetNamedDeclaration;
import org.jetbrains.kotlin.psi.JetNamedDeclarationUtil;
import org.jetbrains.kotlin.resolve.scopes.JetScope;

import java.util.Collection;
import java.util.Collections;

public class ResolveSessionUtils {

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
    public static Collection<ClassDescriptor> getClassDescriptorsByFqName(@NotNull ModuleDescriptor moduleDescriptor, @NotNull FqName fqName) {
        return getClassOrObjectDescriptorsByFqName(moduleDescriptor, fqName, NON_SINGLETON_FILTER);
    }

    @NotNull
    public static Collection<ClassDescriptor> getClassOrObjectDescriptorsByFqName(
            @NotNull ModuleDescriptor moduleDescriptor,
            @NotNull FqName fqName,
            @NotNull Predicate<ClassDescriptor> filter
    ) {
        if (fqName.isRoot()) return Collections.emptyList();

        Collection<ClassDescriptor> classDescriptors = Lists.newArrayList();

        FqName packageFqName = fqName.parent();
        while (true) {
            PackageViewDescriptor packageDescriptor = moduleDescriptor.getPackage(packageFqName);
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
    public static Name safeNameForLazyResolve(@NotNull JetNamedDeclaration declaration) {
        return safeNameForLazyResolve(declaration.getNameAsName());
    }

    @NotNull
    public static Name safeNameForLazyResolve(@Nullable Name name) {
        return SpecialNames.safeIdentifier(name);
    }

    @Nullable
    public static FqName safeFqNameForLazyResolve(@NotNull JetNamedDeclaration declaration) {
        //NOTE: should only create special names for package level declarations, so we can safely rely on real fq name for parent
        FqName parentFqName = JetNamedDeclarationUtil.getParentFqName(declaration);
        return parentFqName != null ? parentFqName.child(safeNameForLazyResolve(declaration)) : null;
    }
}
