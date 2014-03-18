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

package org.jetbrains.jet.plugin.stubindex;

import com.google.common.collect.Sets;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Condition;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.NamePackage;

import java.util.Collection;
import java.util.Set;

public final class PackageIndexUtil {
    @NotNull
    public static Collection<FqName> getSubPackageFqNames(
            @NotNull FqName packageFqName,
            @NotNull GlobalSearchScope scope,
            @NotNull Project project
    ) {
        Collection<JetFile> files = JetAllPackagesIndex.getInstance().get(packageFqName.asString(), project, scope);

        Set<FqName> result = Sets.newHashSet();
        for (JetFile file : files) {
            FqName fqName = file.getPackageFqName();

            assert NamePackage.isSubpackageOf(fqName, packageFqName) :
                    "Registered package is not a subpackage of actually declared package:\n" +
                    "in index: " + packageFqName + "\n" +
                    "declared: " + fqName;
            FqName subpackage = NamePackage.plusOneSegment(packageFqName, fqName);
            if (subpackage != null) {
                result.add(subpackage);
            }
        }

        return result;
    }

    @NotNull
    public static Collection<JetFile> findFilesWithExactPackage(
            @NotNull final FqName packageFqName,
            @NotNull GlobalSearchScope searchScope,
            @NotNull Project project
    ) {
        Collection<JetFile> files = JetAllPackagesIndex.getInstance().get(packageFqName.asString(), project, searchScope);
        return ContainerUtil.filter(files, new Condition<JetFile>() {
            @Override
            public boolean value(JetFile file) {
                return packageFqName.equals(file.getPackageFqName());
            }
        });
    }

    private PackageIndexUtil() {
    }
}
