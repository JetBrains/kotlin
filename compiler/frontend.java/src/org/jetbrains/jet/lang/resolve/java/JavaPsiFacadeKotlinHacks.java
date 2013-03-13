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

import com.google.common.collect.Lists;
import com.intellij.core.CoreJavaFileManager;
import com.intellij.openapi.progress.ProgressIndicatorProvider;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElementFinder;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.impl.JavaPsiFacadeImpl;
import com.intellij.psi.impl.file.impl.JavaFileManager;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * TODO Temporary class until {@link JavaPsiFacadeImpl} hacked.
 *
 * @see JavaPsiFacadeImpl
 */
public class JavaPsiFacadeKotlinHacks {
    public interface KotlinFinderMarker {}

    private final JavaFileManager javaFileManager;
    private final List<PsiElementFinder> extensionPsiElementFinders;

    public JavaPsiFacadeKotlinHacks(@NotNull Project project) {
        this.javaFileManager = findJavaFileManager(project);
        this.extensionPsiElementFinders = Lists.newArrayList();
        for (PsiElementFinder finder : project.getExtensions(PsiElementFinder.EP_NAME)) {
            if (!(finder instanceof KotlinFinderMarker)) {
                this.extensionPsiElementFinders.add(finder);
            }
        }
    }

    @NotNull
    private JavaFileManager findJavaFileManager(@NotNull Project project) {
        JavaFileManager javaFileManager = project.getComponent(JavaFileManager.class);
        if (javaFileManager != null) {
            return javaFileManager;
        }
        javaFileManager = project.getComponent(CoreJavaFileManager.class);
        if (javaFileManager != null) {
            // TODO: why it is not found by JavaFileManager?
            return javaFileManager;
        }
        throw new IllegalStateException("JavaFileManager component is not found in project");
    }

    @Nullable
    public PsiPackage findPackage(@NotNull String qualifiedName) {
        PsiPackage psiPackage = javaFileManager.findPackage(qualifiedName);
        if (psiPackage != null) {
            return psiPackage;
        }

        for (PsiElementFinder finder : extensionPsiElementFinders) {
            psiPackage = finder.findPackage(qualifiedName);
            if (psiPackage != null) {
                return psiPackage;
            }
        }
        return psiPackage;
    }

    public PsiClass findClass(@NotNull String qualifiedName, @NotNull GlobalSearchScope scope) {
        ProgressIndicatorProvider.checkCanceled(); // We hope this method is being called often enough to cancel daemon processes smoothly

        PsiClass aClass = javaFileManager.findClass(qualifiedName, scope);
        if (aClass != null) {
            return aClass;
        }

        for (PsiElementFinder finder : extensionPsiElementFinders) {
            aClass = finder.findClass(qualifiedName, scope);
            if (aClass != null) {
                return aClass;
            }
        }

        return null;
    }

}
