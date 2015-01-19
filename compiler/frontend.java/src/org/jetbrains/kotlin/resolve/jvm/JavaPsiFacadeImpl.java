/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.resolve.jvm;

import com.intellij.openapi.progress.ProgressIndicatorProvider;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElementFinder;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.reference.SoftReference;
import com.intellij.util.ConcurrencyUtil;
import com.intellij.util.containers.ConcurrentHashMap;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

public abstract class JavaPsiFacadeImpl {
    private volatile PsiElementFinder[] myElementFinders;
    private volatile SoftReference<ConcurrentMap<String, PsiPackage>> myPackageCache;
    private final Project myProject;

    public JavaPsiFacadeImpl(Project project) {
        myProject = project;
    }

    public PsiClass findClass(@NotNull String qualifiedName, @NotNull GlobalSearchScope scope) {
        ProgressIndicatorProvider.checkCanceled(); // We hope this method is being called often enough to cancel daemon processes smoothly

        if (DumbService.getInstance(getProject()).isDumb()) {
            PsiClass[] classes = findClassesInDumbMode(qualifiedName, scope);
            if (classes.length != 0) {
                return classes[0];
            }
            return null;
        }

        for (PsiElementFinder finder : finders()) {
            PsiClass aClass = finder.findClass(qualifiedName, scope);
            if (aClass != null) return aClass;
        }

        return null;
    }

    @NotNull
    private PsiClass[] findClassesInDumbMode(@NotNull String qualifiedName, @NotNull GlobalSearchScope scope) {
        String packageName = StringUtil.getPackageName(qualifiedName);
        PsiPackage pkg = findPackage(packageName);
        String className = StringUtil.getShortName(qualifiedName);
        if (pkg == null && packageName.length() < qualifiedName.length()) {
            PsiClass[] containingClasses = findClassesInDumbMode(packageName, scope);
            if (containingClasses.length == 1) {
                return PsiElementFinder.filterByName(className, containingClasses[0].getInnerClasses());
            }

            return PsiClass.EMPTY_ARRAY;
        }

        if (pkg == null || !pkg.containsClassNamed(className)) {
            return PsiClass.EMPTY_ARRAY;
        }

        return pkg.findClassByShortName(className, scope);
    }

    @NotNull
    private PsiElementFinder[] finders() {
        PsiElementFinder[] answer = myElementFinders;
        if (answer == null) {
            answer = calcFinders();
            myElementFinders = answer;
        }

        return answer;
    }

    @NotNull
    protected abstract PsiElementFinder[] calcFinders();

    public PsiPackage findPackage(@NotNull String qualifiedName) {
        ConcurrentMap<String, PsiPackage> cache = SoftReference.dereference(myPackageCache);
        if (cache == null) {
            myPackageCache = new SoftReference<ConcurrentMap<String, PsiPackage>>(cache = new ConcurrentHashMap<String, PsiPackage>());
        }

        PsiPackage aPackage = cache.get(qualifiedName);
        if (aPackage != null) {
            return aPackage;
        }

        for (PsiElementFinder finder : filteredFinders()) {
            aPackage = finder.findPackage(qualifiedName);
            if (aPackage != null) {
                return ConcurrencyUtil.cacheOrGet(cache, qualifiedName, aPackage);
            }
        }

        return null;
    }

    @NotNull
    private PsiElementFinder[] filteredFinders() {
        DumbService dumbService = DumbService.getInstance(getProject());
        PsiElementFinder[] finders = finders();
        if (dumbService.isDumb()) {
            List<PsiElementFinder> list = dumbService.filterByDumbAwareness(Arrays.asList(finders));
            finders = list.toArray(new PsiElementFinder[list.size()]);
        }
        return finders;
    }

    @NotNull
    public Project getProject() {
        return myProject;
    }
}
