/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.load.java;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.search.DelegatingGlobalSearchScope;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.idea.KotlinFileType;
import org.jetbrains.kotlin.load.java.structure.JavaClass;
import org.jetbrains.kotlin.load.java.structure.JavaPackage;
import org.jetbrains.kotlin.load.java.structure.impl.JavaPackageImpl;
import org.jetbrains.kotlin.name.ClassId;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.resolve.BindingTrace;
import org.jetbrains.kotlin.resolve.CodeAnalyzerInitializer;
import org.jetbrains.kotlin.resolve.jvm.KotlinJavaPsiFacade;
import org.jetbrains.kotlin.resolve.lazy.KotlinCodeAnalyzer;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.Set;

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

    public class FilterOutKotlinSourceFilesScope extends DelegatingGlobalSearchScope {
        public FilterOutKotlinSourceFilesScope(@NotNull GlobalSearchScope baseScope) {
            super(baseScope);
        }

        @Override
        public boolean contains(@NotNull VirtualFile file) {
            return myBaseScope.contains(file) && (file.isDirectory() || file.getFileType() != KotlinFileType.INSTANCE);
        }

        @NotNull
        public GlobalSearchScope getBase() {
            return myBaseScope;
        }

        //NOTE: expected by class finder to be not null
        @NotNull
        @Override
        public Project getProject() {
            return project;
        }

        @Override
        public String toString() {
            return "JCFI: " + myBaseScope;
        }
    }

    @PostConstruct
    public void initialize(@NotNull BindingTrace trace, @NotNull KotlinCodeAnalyzer codeAnalyzer) {
        javaSearchScope = new FilterOutKotlinSourceFilesScope(baseScope);
        javaFacade = KotlinJavaPsiFacade.getInstance(project);
        CodeAnalyzerInitializer.Companion.getInstance(project).initialize(trace, codeAnalyzer.getModuleDescriptor(), codeAnalyzer);
    }

    @Nullable
    @Override
    public JavaClass findClass(@NotNull ClassId classId) {
        return javaFacade.findClass(classId, javaSearchScope);
    }

    @Nullable
    @Override
    public JavaPackage findPackage(@NotNull FqName fqName) {
        PsiPackage psiPackage = javaFacade.findPackage(fqName.asString(), javaSearchScope);
        return psiPackage == null ? null : new JavaPackageImpl(psiPackage, javaSearchScope);
    }

    @Nullable
    @Override
    public Set<String> knownClassNamesInPackage(@NotNull FqName packageFqName) {
        return javaFacade.knownClassNamesInPackage(packageFqName);
    }
}
