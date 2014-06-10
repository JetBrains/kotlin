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

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.search.DelegatingGlobalSearchScope;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.resolve.java.structure.JavaClass;
import org.jetbrains.jet.lang.resolve.java.structure.JavaPackage;
import org.jetbrains.jet.lang.resolve.java.structure.impl.JavaClassImpl;
import org.jetbrains.jet.lang.resolve.java.structure.impl.JavaPackageImpl;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.plugin.JetFileType;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

public class JavaClassFinderImpl implements JavaClassFinder {
    @NotNull
    private Project project;

    private GlobalSearchScope javaSearchScope;
    private JavaPsiFacadeKotlinHacks javaFacade;

    @Inject
    public void setProject(@NotNull Project project) {
        this.project = project;
    }

    @PostConstruct
    public void initialize() {
        javaSearchScope = new DelegatingGlobalSearchScope(GlobalSearchScope.allScope(project)) {
            @Override
            public boolean contains(VirtualFile file) {
                return myBaseScope.contains(file) && file.getFileType() != JetFileType.INSTANCE;
            }

            @Override
            public int compare(VirtualFile file1, VirtualFile file2) {
                // TODO: this is a hackish workaround for the following problem:
                // since we are working with the allScope(), if the same class FqName
                // to be on the class path twice, because it is included into different libraries
                // (e.g. junit-4.0.jar is used as a separate library and as a part of idea_full)
                // the two libraries are attached to different modules, the parent compare()
                // can't tell which one comes first, so they can come in random order
                // To fix this, we sort additionally by the full path, to make the ordering deterministic
                // TODO: Delete this hack when proper scopes are used
                int compare = super.compare(file1, file2);
                if (compare == 0) {
                    return Comparing.compare(file1.getPath(), file2.getPath());
                }
                return compare;
            }

            //NOTE: expected by class finder to be not null
            @NotNull
            @Override
            public Project getProject() {
                return project;
            }
        };
        javaFacade = new JavaPsiFacadeKotlinHacks(project);
    }

    @Nullable
    @Override
    public JavaClass findClass(@NotNull FqName fqName) {
        PsiClass psiClass = javaFacade.findClass(fqName.asString(), javaSearchScope);
        if (psiClass == null) return null;

        JavaClassImpl javaClass = new JavaClassImpl(psiClass);

        if (!fqName.equals(javaClass.getFqName())) {
            throw new IllegalStateException("Requested " + fqName + ", got " + javaClass.getFqName());
        }

        if (javaClass.getOriginKind() == JavaClass.OriginKind.KOTLIN_LIGHT_CLASS) {
            throw new IllegalStateException("Kotlin light classes should not be found by JavaPsiFacade, resolving: " + fqName);
        }

        return javaClass;
    }

    @Nullable
    @Override
    public JavaPackage findPackage(@NotNull FqName fqName) {
        PsiPackage psiPackage = javaFacade.findPackage(fqName.asString());
        return psiPackage == null ? null : new JavaPackageImpl(psiPackage);
    }
}
