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

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Ref;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.stubs.StubIndex;
import com.intellij.util.Processor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.name.FqName;

import java.util.Collection;

public final class PackageIndexUtil {
    @NotNull
    public static Collection<FqName> getSubPackageFqNames(
            @NotNull FqName packageFqName,
            @NotNull GlobalSearchScope scope,
            @NotNull Project project
    ) {
        return SubpackagesIndexService.OBJECT$.getInstance(project).getSubpackages(packageFqName, scope);
    }

    @NotNull
    public static Collection<JetFile> findFilesWithExactPackage(
            @NotNull FqName packageFqName,
            @NotNull GlobalSearchScope searchScope,
            @NotNull Project project
    ) {
        return JetExactPackagesIndex.getInstance().get(packageFqName.asString(), project, searchScope);
    }

    public static boolean packageExists(
            @NotNull FqName packageFqName,
            @NotNull GlobalSearchScope searchScope,
            @NotNull Project project
    ) {
        final Ref<Boolean> result = new Ref<Boolean>(false);
        StubIndex.getInstance().processElements(
                JetAllPackagesIndex.getInstance().getKey(), packageFqName.asString(), project, searchScope, JetFile.class,
                new Processor<JetFile>() {
                    @Override
                    public boolean process(JetFile file) {
                        result.set(true);
                        return false;
                    }
                });
        return result.get();
    }

    private PackageIndexUtil() {
    }
}
