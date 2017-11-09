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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.ClassDescriptor;
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor;
import org.jetbrains.kotlin.descriptors.ModuleDescriptor;
import org.jetbrains.kotlin.descriptors.PackageViewDescriptor;
import org.jetbrains.kotlin.incremental.components.NoLookupLocation;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.FqNamesUtilKt;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.name.SpecialNames;
import org.jetbrains.kotlin.psi.KtNamedDeclaration;
import org.jetbrains.kotlin.psi.KtNamedDeclarationUtil;
import org.jetbrains.kotlin.resolve.scopes.MemberScope;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Predicate;

public class ResolveSessionUtils {

    private ResolveSessionUtils() {
    }

    @NotNull
    public static Collection<ClassDescriptor> getClassDescriptorsByFqName(@NotNull ModuleDescriptor module, @NotNull FqName fqName) {
        return getClassOrObjectDescriptorsByFqName(module, fqName, descriptor -> true);
    }

    @NotNull
    private static Collection<ClassDescriptor> getClassOrObjectDescriptorsByFqName(
            @NotNull ModuleDescriptor module,
            @NotNull FqName fqName,
            @NotNull Predicate<ClassDescriptor> filter
    ) {
        if (fqName.isRoot()) return Collections.emptyList();

        Collection<ClassDescriptor> result = new ArrayList<>(1);

        FqName packageFqName = fqName.parent();
        while (true) {
            PackageViewDescriptor packageDescriptor = module.getPackage(packageFqName);
            if (!packageDescriptor.isEmpty()) {
                FqName relativeClassFqName = FqNamesUtilKt.tail(fqName, packageFqName);
                ClassDescriptor classDescriptor = findClassByRelativePath(packageDescriptor.getMemberScope(), relativeClassFqName);
                if (classDescriptor != null && filter.test(classDescriptor)) {
                    result.add(classDescriptor);
                }
            }

            if (packageFqName.isRoot()) {
                break;
            }

            packageFqName = packageFqName.parent();
        }

        return result;
    }

    @Nullable
    public static ClassDescriptor findClassByRelativePath(@NotNull MemberScope packageScope, @NotNull FqName path) {
        if (path.isRoot()) return null;

        MemberScope scope = packageScope;
        ClassifierDescriptor classifier = null;
        for (Name name : path.pathSegments()) {
            classifier = scope.getContributedClassifier(name, NoLookupLocation.WHEN_FIND_BY_FQNAME);
            if (!(classifier instanceof ClassDescriptor)) return null;
            scope = ((ClassDescriptor) classifier).getUnsubstitutedInnerClassesScope();
        }

        return (ClassDescriptor) classifier;
    }

    @NotNull
    public static Name safeNameForLazyResolve(@NotNull KtNamedDeclaration declaration) {
        return safeNameForLazyResolve(declaration.getNameAsName());
    }

    @NotNull
    public static Name safeNameForLazyResolve(@Nullable Name name) {
        return SpecialNames.safeIdentifier(name);
    }

    @Nullable
    public static FqName safeFqNameForLazyResolve(@NotNull KtNamedDeclaration declaration) {
        //NOTE: should only create special names for package level declarations, so we can safely rely on real fq name for parent
        FqName parentFqName = KtNamedDeclarationUtil.getParentFqName(declaration);
        return parentFqName != null ? parentFqName.child(safeNameForLazyResolve(declaration)) : null;
    }
}
