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

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.Function;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.name.FqName;

import java.util.Collection;

public abstract class JetFilesProvider {
    public static JetFilesProvider getInstance(Project project) {
        return ServiceManager.getService(project, JetFilesProvider.class);
    }

    public final Collection<JetFile> allPackageFiles(@NotNull JetFile file) {
        final FqName name = file.getPackageFqName();
        return Collections2.filter(sampleToAllFilesInModule().fun(file), new Predicate<PsiFile>() {
            @Override
            public boolean apply(PsiFile psiFile) {
                return ((JetFile) psiFile).getPackageFqName().equals(name);
            }
        });
    }

    public abstract Function<JetFile, Collection<JetFile>> sampleToAllFilesInModule();
    @NotNull
    public abstract Collection<JetFile> allInScope(@NotNull GlobalSearchScope scope);
    public abstract boolean isFileInScope(@NotNull JetFile file, @NotNull GlobalSearchScope scope);
}
