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

import com.intellij.core.CoreJavaFileManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.PackageIndex;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.impl.file.PsiPackageImpl;
import com.intellij.psi.impl.file.impl.JavaFileManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.CommonProcessors;
import com.intellij.util.Processor;
import com.intellij.util.Query;
import kotlin.Function1;
import kotlin.KotlinPackage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class KotlinJavaPsiFacade {
    private final KotlinJavaPsiFacadeWrapper javaPsiFacadeWrapper;

    public KotlinJavaPsiFacade(Project project, GlobalSearchScope searchScope) {
        javaPsiFacadeWrapper = new KotlinJavaPsiFacadeWrapper(project, searchScope);
    }

    public PsiClass findClass(@NotNull String qualifiedName, @NotNull GlobalSearchScope scope) {
        return javaPsiFacadeWrapper.findClass(qualifiedName, scope);
    }

    public PsiPackage findPackage(@NotNull String qualifiedName) {
        return javaPsiFacadeWrapper.findPackage(qualifiedName);
    }

    static class KotlinJavaPsiFacadeWrapper extends JavaPsiFacadeImpl {
        private final GlobalSearchScope searchScope;

        public KotlinJavaPsiFacadeWrapper(Project project, GlobalSearchScope searchScope) {
            super(project, PsiManager.getInstance(project), findJavaFileManager(project), null);
            this.searchScope = searchScope;
        }

        @NotNull
        @Override
        protected PsiElementFinder[] calcFinders() {
            List<PsiElementFinder> filteredBaseFinders = KotlinPackage.filter(
                    super.calcFinders(), new Function1<PsiElementFinder, Boolean>() {
                @Override
                public Boolean invoke(PsiElementFinder finder) {
                    if (finder instanceof KotlinFinderMarker) return false;

                    if (finder.getClass().getName().equals("org.jetbrains.kotlin.resolve.jvm.JavaPsiFacadeImpl$PsiElementFinderImpl")) {
                        // TODO: Replace with instanceof check in idea 14
                        return false;
                    }

                    return true;
                }
            });

            List<PsiElementFinder> elementFinders = new ArrayList<PsiElementFinder>();
            elementFinders.add(new KotlinPsiElementFinderImpl(getProject(), searchScope));
            elementFinders.addAll(filteredBaseFinders);

            return elementFinders.toArray(new PsiElementFinder[elementFinders.size()]);
        }

        @NotNull
        private static JavaFileManager findJavaFileManager(@NotNull Project project) {
            JavaFileManager javaFileManager = ServiceManager.getService(project, JavaFileManager.class);
            if (javaFileManager == null) {
                throw new IllegalStateException("JavaFileManager component is not found in project");
            }

            return javaFileManager;
        }

        static class KotlinPsiElementFinderImpl extends PsiElementFinder implements DumbAware {
            private final GlobalSearchScope searchScope;

            private final JavaFileManager javaFileManager;
            private final boolean isCoreJavaFileManager;

            private final PsiManager psiManager;
            private final PackageIndex packageIndex;

            public KotlinPsiElementFinderImpl(Project project, GlobalSearchScope searchScope) {
                this.searchScope = searchScope;

                this.javaFileManager = findJavaFileManager(project);
                this.isCoreJavaFileManager = javaFileManager instanceof CoreJavaFileManager;

                this.packageIndex = PackageIndex.getInstance(project);
                this.psiManager = PsiManager.getInstance(project);
            }

            @Override
            public PsiClass findClass(@NotNull String qualifiedName, @NotNull GlobalSearchScope scope) {
                PsiClass aClass = javaFileManager.findClass(qualifiedName, scope);
                if (aClass != null) {
                    //TODO: (module refactoring) CoreJavaFileManager should check scope
                    if (!isCoreJavaFileManager || scope.contains(aClass.getContainingFile().getOriginalFile().getVirtualFile())) {
                        return aClass;
                    }
                }

                return null;
            }

            @Override
            public PsiPackage findPackage(@NotNull String qualifiedName) {
                if (isCoreJavaFileManager) {
                    return javaFileManager.findPackage(qualifiedName);
                }

                Query<VirtualFile> dirs = packageIndex.getDirsByPackageName(qualifiedName, true);
                return hasDirectoriesInScope(dirs) ? new PsiPackageImpl(psiManager, qualifiedName) : null;
            }

            private boolean hasDirectoriesInScope(Query<VirtualFile> dirs) {
                CommonProcessors.FindProcessor<VirtualFile> findProcessor = new CommonProcessors.FindProcessor<VirtualFile>() {
                    @Override
                    protected boolean accept(VirtualFile file) {
                        return searchScope.accept(file);
                    }
                };

                dirs.forEach(findProcessor);
                return findProcessor.isFound();
            }

            @Override
            @NotNull
            public PsiClass[] findClasses(@NotNull String qualifiedName, @NotNull GlobalSearchScope scope) {
                throw new UnsupportedOperationException();
            }

            @Override
            @NotNull
            public PsiPackage[] getSubPackages(@NotNull PsiPackage psiPackage, @NotNull GlobalSearchScope scope) {
                throw new UnsupportedOperationException();
            }

            @Override
            @NotNull
            public PsiClass[] getClasses(@NotNull PsiPackage psiPackage, @NotNull GlobalSearchScope scope) {
                throw new UnsupportedOperationException();
            }

            @Override
            @NotNull
            public PsiClass[] getClasses(@Nullable String shortName, @NotNull PsiPackage psiPackage, @NotNull GlobalSearchScope scope) {
                throw new UnsupportedOperationException();
            }

            @NotNull
            @Override
            public Set<String> getClassNames(@NotNull PsiPackage psiPackage, @NotNull GlobalSearchScope scope) {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean processPackageDirectories(@NotNull PsiPackage psiPackage,
                    @NotNull GlobalSearchScope scope,
                    @NotNull Processor<PsiDirectory> consumer,
                    boolean includeLibrarySources) {
                throw new UnsupportedOperationException();
            }
        }
    }
}
