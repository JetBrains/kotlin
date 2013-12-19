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

package org.jetbrains.jet.lang.resolve.lazy;

import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.ClassifierDescriptor;
import org.jetbrains.jet.lang.descriptors.PackageViewDescriptor;
import org.jetbrains.jet.lang.psi.JetNamed;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.util.QualifiedNamesUtil;

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

    private ResolveSessionUtils() {
    }

    @NotNull
    public static Collection<ClassDescriptor> getClassDescriptorsByFqName(@NotNull KotlinCodeAnalyzer analyzer, @NotNull FqName fqName) {
        return getClassOrObjectDescriptorsByFqName(analyzer, fqName, false);
    }

    @NotNull
    public static Collection<ClassDescriptor> getClassOrObjectDescriptorsByFqName(
            @NotNull KotlinCodeAnalyzer analyzer,
            @NotNull FqName fqName,
            boolean includeObjectDeclarations
    ) {
        if (fqName.isRoot()) {
            return Collections.emptyList();
        }

        Collection<ClassDescriptor> classDescriptors = Lists.newArrayList();

        FqName packageFqName = fqName.parent();
        while (true) {
            PackageViewDescriptor packageDescriptor = analyzer.getModuleDescriptor().getPackage(packageFqName);
            if (packageDescriptor != null) {
                FqName classInPackagePath = new FqName(QualifiedNamesUtil.tail(packageFqName, fqName));
                Collection<ClassDescriptor> descriptors = getClassOrObjectDescriptorsByFqName(packageDescriptor, classInPackagePath,
                                                                                              includeObjectDeclarations);
                classDescriptors.addAll(descriptors);
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

    @NotNull
    private static Collection<ClassDescriptor> getClassOrObjectDescriptorsByFqName(
            @NotNull PackageViewDescriptor packageDescriptor,
            @NotNull FqName path,
            boolean includeObjectDeclarations
    ) {
        if (path.isRoot()) {
            return Collections.emptyList();
        }

        Collection<JetScope> scopes = Arrays.asList(packageDescriptor.getMemberScope());

        List<Name> names = path.pathSegments();
        if (names.size() > 1) {
            for (Name subName : path.pathSegments().subList(0, names.size() - 1)) {
                Collection<JetScope> tempScopes = Lists.newArrayList();
                for (JetScope scope : scopes) {
                    ClassifierDescriptor classifier = scope.getClassifier(subName);
                    if (classifier instanceof ClassDescriptor) {
                        tempScopes.add(((ClassDescriptor) classifier).getUnsubstitutedInnerClassesScope());
                    }
                }
                scopes = tempScopes;
            }
        }

        Name shortName = path.shortName();
        Collection<ClassDescriptor> resultClassifierDescriptors = Lists.newArrayList();
        for (JetScope scope : scopes) {
            ClassifierDescriptor classifier = scope.getClassifier(shortName);
            if (classifier instanceof ClassDescriptor &&
                includeObjectDeclarations == ((ClassDescriptor) classifier).getKind().isSingleton()) {
                resultClassifierDescriptors.add((ClassDescriptor) classifier);
            }
        }

        return resultClassifierDescriptors;
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
