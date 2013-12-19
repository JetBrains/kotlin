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
import com.intellij.psi.*;
import com.intellij.psi.impl.JavaPsiFacadeImpl;
import com.intellij.psi.impl.file.impl.JavaFileManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.HashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

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

    public PsiPackage[] getSubPackages(@NotNull PsiPackage psiPackage) {
        GlobalSearchScope scope = GlobalSearchScope.allScope(psiPackage.getProject());

        LinkedHashSet<PsiPackage> result = new LinkedHashSet<PsiPackage>();
        for (PsiElementFinder finder : extensionPsiElementFinders) {
            PsiPackage[] packages = finder.getSubPackages(psiPackage, scope);
            ContainerUtil.addAll(result, packages);
        }
        ContainerUtil.addAll(result, getDefaultSubPackages(psiPackage, scope));

        return result.toArray(new PsiPackage[result.size()]);
    }

    private static PsiPackage[] getDefaultSubPackages(@NotNull PsiPackage psiPackage, @NotNull GlobalSearchScope scope) {
        Map<String, PsiPackage> packagesMap = new HashMap<String, PsiPackage>();
        String qualifiedName = psiPackage.getQualifiedName();
        for (PsiDirectory dir : psiPackage.getDirectories(scope)) {
            PsiDirectory[] subDirs = dir.getSubdirectories();
            for (PsiDirectory subDir : subDirs) {
                PsiPackage aPackage = JavaDirectoryService.getInstance().getPackage(subDir);
                if (aPackage != null) {
                    String subQualifiedName = aPackage.getQualifiedName();
                    if (subQualifiedName.startsWith(qualifiedName) && !packagesMap.containsKey(subQualifiedName)) {
                        packagesMap.put(aPackage.getQualifiedName(), aPackage);
                    }
                }
            }
        }

        packagesMap.remove(qualifiedName);    // avoid SOE caused by returning a package as a subpackage of itself
        return packagesMap.values().toArray(new PsiPackage[packagesMap.size()]);
    }
}
