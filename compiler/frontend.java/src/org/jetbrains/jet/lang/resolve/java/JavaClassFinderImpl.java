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
import org.jetbrains.kotlin.name.ClassId;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.FqNameUnsafe;
import org.jetbrains.jet.plugin.JetFileType;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

public class JavaClassFinderImpl implements JavaClassFinder {
    private Project project;
    private GlobalSearchScope baseScope;

    private GlobalSearchScope javaSearchScope;
    private KotlinJavaPsiFacade javaFacade;

    @Inject
    public void setProject(@NotNull Project project) {
        this.project = project;
    }

    @Inject
    public void setScope(@NotNull GlobalSearchScope scope) {
        this.baseScope = scope;
    }

    @Inject
    public void setComponentPostConstruct(@NotNull JavaClassFinderPostConstruct finderPostConstruct) {
        // Only activate post create
    }

    @PostConstruct
    public void initialize() {
        javaSearchScope = new DelegatingGlobalSearchScope(baseScope) {
            @Override
            public boolean contains(@NotNull VirtualFile file) {
                return myBaseScope.contains(file) && (file.isDirectory() || file.getFileType() != JetFileType.INSTANCE);
            }

            //NOTE: expected by class finder to be not null
            @NotNull
            @Override
            public Project getProject() {
                return project;
            }
        };
        javaFacade = new KotlinJavaPsiFacade(project, javaSearchScope);
    }

    @Nullable
    @Override
    public JavaClass findClass(@NotNull ClassId classId) {
        FqNameUnsafe fqName = classId.asSingleFqName();

        PsiClass psiClass = javaFacade.findClass(fqName.asString(), javaSearchScope);
        if (psiClass == null) return null;

        JavaClassImpl javaClass = new JavaClassImpl(psiClass);

        if (!fqName.equalsTo(javaClass.getFqName())) {
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
        return psiPackage == null ? null : new JavaPackageImpl(psiPackage, javaSearchScope);
    }
}
