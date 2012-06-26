/*
 * Copyright 2010-2012 JetBrains s.r.o.
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
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.impl.compiled.ClsClassImpl;
import com.intellij.psi.search.DelegatingGlobalSearchScope;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.java.alt.AltClassFinder;
import org.jetbrains.jet.plugin.JetFileType;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

/**
 * @author Stepan Koltsov
 */
public class PsiClassFinderForJvm implements PsiClassFinder {

    @NotNull
    private Project project;
    @NotNull
    private CompilerDependencies compilerDependencies;

    private AltClassFinder altClassFinder;
    private GlobalSearchScope javaSearchScope;
    private JavaPsiFacadeKotlinHacks javaFacade;

    @Inject
    public void setProject(@NotNull Project project) {
        this.project = project;
    }

    @Inject
    public void setCompilerDependencies(@NotNull CompilerDependencies compilerDependencies) {
        this.compilerDependencies = compilerDependencies;
    }

    @PostConstruct
    public void initialize() {
        this.altClassFinder = new AltClassFinder(project, compilerDependencies.getJdkHeaderRoots());
        this.javaSearchScope = new DelegatingGlobalSearchScope(GlobalSearchScope.allScope(project)) {
            @Override
            public boolean contains(VirtualFile file) {
                return myBaseScope.contains(file) && file.getFileType() != JetFileType.INSTANCE;
            }
        };
        this.javaFacade = new JavaPsiFacadeKotlinHacks(project);
    }


    @Override
    @Nullable
    public PsiClass findPsiClass(@NotNull FqName qualifiedName, @NotNull RuntimeClassesHandleMode runtimeClassesHandleMode) {
        PsiClass original = javaFacade.findClass(qualifiedName.getFqName(), javaSearchScope);
        PsiClass altClass = altClassFinder.findClass(qualifiedName);

        PsiClass result = original;
        if (altClass != null) {
            if (original == null) {
                return null;
            }

            if (altClass instanceof ClsClassImpl) {
                altClass.putUserData(ClsClassImpl.DELEGATE_KEY, original);
            }

            result = altClass;
        }

        if (result != null) {
            FqName actualQualifiedName = new FqName(result.getQualifiedName());
            if (!actualQualifiedName.equals(qualifiedName)) {
                throw new IllegalStateException("requested " + qualifiedName + ", got " + actualQualifiedName);
            }
        }

        if (result instanceof JetJavaMirrorMarker) {
            throw new IllegalStateException("JetJavaMirrorMaker is not possible in resolve.java, resolving: " + qualifiedName);
        }

        if (result == null) {
            return null;
        }

        PsiAnnotation assertInvisibleAnnotation = JavaDescriptorResolver
                .findAnnotation(result, JvmStdlibNames.ASSERT_INVISIBLE_IN_RESOLVER.getFqName().getFqName());
        if (assertInvisibleAnnotation != null) {
            if (runtimeClassesHandleMode == RuntimeClassesHandleMode.IGNORE) {
                return null;
            }
            else if (runtimeClassesHandleMode == RuntimeClassesHandleMode.THROW) {
                throw new IllegalStateException(
                        "classpath is configured incorrectly:" +
                        " class " + qualifiedName + " from runtime must not be loaded by compiler");
            }
            else {
                throw new IllegalStateException("unknown parameter value: " + runtimeClassesHandleMode);
            }
        }

        return result;
    }

    @Override
    @Nullable
    public PsiPackage findPsiPackage(@NotNull FqName qualifiedName) {
        return javaFacade.findPackage(qualifiedName.getFqName());
    }


}
