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

package org.jetbrains.kotlin.asJava;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.Sets;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.*;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.SLRUCache;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.load.kotlin.PackageClassUtils;
import org.jetbrains.kotlin.load.kotlin.PackagePartClassUtils;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.NamePackage;
import org.jetbrains.kotlin.psi.JetClassOrObject;
import org.jetbrains.kotlin.psi.JetEnumEntry;
import org.jetbrains.kotlin.psi.JetFile;
import org.jetbrains.kotlin.resolve.jvm.KotlinFinderMarker;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public class JavaElementFinder extends PsiElementFinder implements KotlinFinderMarker {

    @NotNull
    public static JavaElementFinder getInstance(@NotNull Project project) {
        PsiElementFinder[] extensions = Extensions.getArea(project).getExtensionPoint(PsiElementFinder.EP_NAME).getExtensions();
        for (PsiElementFinder extension : extensions) {
            if (extension instanceof JavaElementFinder) {
                return (JavaElementFinder) extension;
            }
        }
        throw new IllegalStateException(JavaElementFinder.class.getSimpleName() + " is not found for project " + project);
    }

    private final Project project;
    private final PsiManager psiManager;
    private final LightClassGenerationSupport lightClassGenerationSupport;

    private final CachedValue<SLRUCache<FindClassesRequest, PsiClass[]>> findClassesCache;

    public JavaElementFinder(
            @NotNull Project project,
            @NotNull LightClassGenerationSupport lightClassGenerationSupport
    ) {
        this.project = project;
        this.psiManager = PsiManager.getInstance(project);
        this.lightClassGenerationSupport = lightClassGenerationSupport;
        this.findClassesCache = CachedValuesManager.getManager(project).createCachedValue(
            new CachedValueProvider<SLRUCache<FindClassesRequest, PsiClass[]>>() {
                @Nullable
                @Override
                public Result<SLRUCache<FindClassesRequest, PsiClass[]>> compute() {
                    return new Result<SLRUCache<FindClassesRequest, PsiClass[]>>(
                            new SLRUCache<FindClassesRequest, PsiClass[]>(30, 10) {
                                @NotNull
                                @Override
                                public PsiClass[] createValue(FindClassesRequest key) {
                                    return doFindClasses(key.fqName, key.scope);
                                }
                            },
                            PsiModificationTracker.OUT_OF_CODE_BLOCK_MODIFICATION_COUNT
                    );
                }
            },
            false
        );
    }

    @Override
    public PsiClass findClass(@NotNull String qualifiedName, @NotNull GlobalSearchScope scope) {
        PsiClass[] allClasses = findClasses(qualifiedName, scope);
        return allClasses.length > 0 ? allClasses[0] : null;
    }

    @NotNull
    @Override
    public PsiClass[] findClasses(@NotNull String qualifiedNameString, @NotNull GlobalSearchScope scope) {
        SLRUCache<FindClassesRequest, PsiClass[]> value = findClassesCache.getValue();
        synchronized (value) {
            return value.get(new FindClassesRequest(qualifiedNameString, scope));
        }
    }

    private PsiClass[] doFindClasses(String qualifiedNameString, GlobalSearchScope scope) {
        if (!NamePackage.isValidJavaFqName(qualifiedNameString)) {
            return PsiClass.EMPTY_ARRAY;
        }

        List<PsiClass> answer = new SmartList<PsiClass>();

        FqName qualifiedName = new FqName(qualifiedNameString);

        findClassesAndObjects(qualifiedName, scope, answer);

        if (PackagePartClassUtils.isFacadeClassFqName(qualifiedName)) {
            answer.addAll(lightClassGenerationSupport.getFacadeClasses(qualifiedName, scope));
        }
        else if (PackageClassUtils.isPackageClassFqName(qualifiedName)) {
            answer.addAll(lightClassGenerationSupport.getPackageClasses(qualifiedName.parent(), scope));
        }

        return sortByClasspath(answer, scope).toArray(new PsiClass[answer.size()]);
    }

    // Finds explicitly declared classes and objects, not package classes
    private void findClassesAndObjects(FqName qualifiedName, GlobalSearchScope scope, List<PsiClass> answer) {
        Collection<JetClassOrObject> classOrObjectDeclarations =
                lightClassGenerationSupport.findClassOrObjectDeclarations(qualifiedName, scope);

        for (JetClassOrObject declaration : classOrObjectDeclarations) {
            if (!(declaration instanceof JetEnumEntry)) {
                PsiClass lightClass = LightClassUtil.getPsiClass(declaration);
                if (lightClass != null) {
                    answer.add(lightClass);
                }
            }
        }
    }

    @NotNull
    @Override
    public Set<String> getClassNames(@NotNull PsiPackage psiPackage, @NotNull GlobalSearchScope scope) {
        FqName packageFQN = new FqName(psiPackage.getQualifiedName());

        Collection<JetClassOrObject> declarations = lightClassGenerationSupport.findClassOrObjectDeclarationsInPackage(packageFQN, scope);

        Set<String> answer = Sets.newHashSet();
        answer.add(PackageClassUtils.getPackageClassName(packageFQN));

        for (JetClassOrObject declaration : declarations) {
            String name = declaration.getName();
            if (name != null) {
                answer.add(name);
            }
        }

        return answer;
    }

    @Override
    public PsiPackage findPackage(@NotNull String qualifiedNameString) {
        if (!NamePackage.isValidJavaFqName(qualifiedNameString)) {
            return null;
        }

        FqName fqName = new FqName(qualifiedNameString);

        // allScope() because the contract says that the whole project
        GlobalSearchScope allScope = GlobalSearchScope.allScope(project);
        if (lightClassGenerationSupport.packageExists(fqName, allScope)) {
            return new JetLightPackage(psiManager, fqName, allScope);
        }

        return null;
    }

    @NotNull
    @Override
    public PsiPackage[] getSubPackages(@NotNull PsiPackage psiPackage, @NotNull final GlobalSearchScope scope) {
        FqName packageFQN = new FqName(psiPackage.getQualifiedName());

        Collection<FqName> subpackages = lightClassGenerationSupport.getSubPackages(packageFQN, scope);

        Collection<PsiPackage> answer = Collections2.transform(subpackages, new Function<FqName, PsiPackage>() {
            @Override
            public PsiPackage apply(@Nullable FqName input) {
                return new JetLightPackage(psiManager, input, scope);
            }
        });

        return answer.toArray(new PsiPackage[answer.size()]);
    }

    @NotNull
    @Override
    public PsiClass[] getClasses(@NotNull PsiPackage psiPackage, @NotNull GlobalSearchScope scope) {
        List<PsiClass> answer = new SmartList<PsiClass>();
        FqName packageFQN = new FqName(psiPackage.getQualifiedName());

        answer.addAll(lightClassGenerationSupport.getPackageClasses(packageFQN, scope));

        Collection<JetClassOrObject> declarations = lightClassGenerationSupport.findClassOrObjectDeclarationsInPackage(packageFQN, scope);
        for (JetClassOrObject declaration : declarations) {
            PsiClass aClass = LightClassUtil.getPsiClass(declaration);
            if (aClass != null) {
                answer.add(aClass);
            }
        }

        return sortByClasspath(answer, scope).toArray(new PsiClass[answer.size()]);
    }

    @Override
    @NotNull
    public PsiFile[] getPackageFiles(@NotNull PsiPackage psiPackage, @NotNull GlobalSearchScope scope) {
        FqName packageFQN = new FqName(psiPackage.getQualifiedName());
        Collection<JetFile> result = lightClassGenerationSupport.findFilesForPackage(packageFQN, scope);
        return result.toArray(new PsiFile[result.size()]);
    }

    @Override
    @Nullable
    public Condition<PsiFile> getPackageFilesFilter(@NotNull final PsiPackage psiPackage, @NotNull GlobalSearchScope scope) {
        return new Condition<PsiFile>() {
            @Override
            public boolean value(PsiFile input) {
                if (!(input instanceof JetFile)) {
                    return true;
                }
                return psiPackage.getQualifiedName().equals(((JetFile) input).getPackageFqName().asString());
            }
        };
    }

    private static class FindClassesRequest {
        private final String fqName;
        private final GlobalSearchScope scope;

        private FindClassesRequest(@NotNull String fqName, @NotNull GlobalSearchScope scope) {
            this.fqName = fqName;
            this.scope = scope;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            FindClassesRequest request = (FindClassesRequest) o;

            if (!fqName.equals(request.fqName)) return false;
            if (!scope.equals(request.scope)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = fqName.hashCode();
            result = 31 * result + (scope.hashCode());
            return result;
        }

        @Override
        public String toString() {
            return fqName + " in " + scope;
        }
    }

    @NotNull
    public static Comparator<PsiElement> byClasspathComparator(@NotNull final GlobalSearchScope searchScope) {
        return new Comparator<PsiElement>() {
            @Override
            public int compare(@NotNull PsiElement o1, @NotNull PsiElement o2) {
                VirtualFile f1 = PsiUtilCore.getVirtualFile(o1);
                VirtualFile f2 = PsiUtilCore.getVirtualFile(o2);
                if (f1 == f2) return 0;
                if (f1 == null) return -1;
                if (f2 == null) return 1;
                return searchScope.compare(f2, f1);
            }
        };
    }

    private static Collection<PsiClass> sortByClasspath(@NotNull List<PsiClass> classes, @NotNull GlobalSearchScope searchScope) {
        if (classes.size() > 1) {
            ContainerUtil.quickSort(classes, byClasspathComparator(searchScope));
        }

        return classes;
    }
}
