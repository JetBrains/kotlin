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

package org.jetbrains.kotlin.asJava.finder;

import com.google.common.collect.Collections2;
import com.google.common.collect.Sets;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.asJava.KotlinAsJavaSupport;
import org.jetbrains.kotlin.load.java.JvmAbi;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.FqNamesUtilKt;
import org.jetbrains.kotlin.psi.KtClass;
import org.jetbrains.kotlin.psi.KtClassOrObject;
import org.jetbrains.kotlin.psi.KtEnumEntry;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.resolve.jvm.KotlinFinderMarker;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import static org.jetbrains.kotlin.asJava.LightClassUtilsKt.toLightClass;

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
    private final KotlinAsJavaSupport kotlinAsJavaSupport;

    public JavaElementFinder(
            @NotNull Project project,
            @NotNull KotlinAsJavaSupport kotlinAsJavaSupport
    ) {
        this.project = project;
        this.psiManager = PsiManager.getInstance(project);
        this.kotlinAsJavaSupport = kotlinAsJavaSupport;
    }

    @Override
    public PsiClass findClass(@NotNull String qualifiedName, @NotNull GlobalSearchScope scope) {
        PsiClass[] allClasses = findClasses(qualifiedName, scope);
        return allClasses.length > 0 ? allClasses[0] : null;
    }

    @NotNull
    @Override
    public PsiClass[] findClasses(@NotNull String qualifiedNameString, @NotNull GlobalSearchScope scope) {
        if (!FqNamesUtilKt.isValidJavaFqName(qualifiedNameString)) {
            return PsiClass.EMPTY_ARRAY;
        }

        List<PsiClass> answer = new SmartList<>();

        FqName qualifiedName = new FqName(qualifiedNameString);

        findClassesAndObjects(qualifiedName, scope, answer);
        answer.addAll(kotlinAsJavaSupport.getFacadeClasses(qualifiedName, scope));
        answer.addAll(kotlinAsJavaSupport.getKotlinInternalClasses(qualifiedName, scope));

        return sortByClasspath(answer, scope).toArray(new PsiClass[answer.size()]);
    }

    // Finds explicitly declared classes and objects, not package classes
    // Also DefaultImpls classes of interfaces
    private void findClassesAndObjects(FqName qualifiedName, GlobalSearchScope scope, List<PsiClass> answer) {
        findInterfaceDefaultImpls(qualifiedName, scope, answer);

        Collection<KtClassOrObject> classOrObjectDeclarations =
                kotlinAsJavaSupport.findClassOrObjectDeclarations(qualifiedName, scope);

        for (KtClassOrObject declaration : classOrObjectDeclarations) {
            if (!(declaration instanceof KtEnumEntry)) {
                PsiClass lightClass = toLightClass(declaration);
                if (lightClass != null) {
                    answer.add(lightClass);
                }
            }
        }
    }

    private void findInterfaceDefaultImpls(FqName qualifiedName, GlobalSearchScope scope, List<PsiClass> answer) {
        if (qualifiedName.isRoot()) return;

        if (!qualifiedName.shortName().asString().equals(JvmAbi.DEFAULT_IMPLS_CLASS_NAME)) return;

        for (KtClassOrObject classOrObject : kotlinAsJavaSupport.findClassOrObjectDeclarations(qualifiedName.parent(), scope)) {
            //NOTE: can't filter out more interfaces right away because decompiled declarations do not have member bodies
            if (classOrObject instanceof KtClass && ((KtClass) classOrObject).isInterface()) {
                PsiClass interfaceClass = toLightClass(classOrObject);
                if (interfaceClass != null) {
                    PsiClass implsClass = interfaceClass.findInnerClassByName(JvmAbi.DEFAULT_IMPLS_CLASS_NAME, false);
                    if (implsClass != null) {
                        answer.add(implsClass);
                    }
                }
            }
        }
    }

    @NotNull
    @Override
    public Set<String> getClassNames(@NotNull PsiPackage psiPackage, @NotNull GlobalSearchScope scope) {
        FqName packageFQN = new FqName(psiPackage.getQualifiedName());

        Collection<KtClassOrObject> declarations = kotlinAsJavaSupport.findClassOrObjectDeclarationsInPackage(packageFQN, scope);

        Set<String> answer = Sets.newHashSet();
        answer.addAll(kotlinAsJavaSupport.getFacadeNames(packageFQN, scope));

        for (KtClassOrObject declaration : declarations) {
            String name = declaration.getName();
            if (name != null) {
                answer.add(name);
            }
        }

        return answer;
    }

    @Override
    public PsiPackage findPackage(@NotNull String qualifiedNameString) {
        if (!FqNamesUtilKt.isValidJavaFqName(qualifiedNameString)) {
            return null;
        }

        FqName fqName = new FqName(qualifiedNameString);

        // allScope() because the contract says that the whole project
        GlobalSearchScope allScope = GlobalSearchScope.allScope(project);
        if (kotlinAsJavaSupport.packageExists(fqName, allScope)) {
            return new KtLightPackage(psiManager, fqName, allScope);
        }

        return null;
    }

    @NotNull
    @Override
    public PsiPackage[] getSubPackages(@NotNull PsiPackage psiPackage, @NotNull GlobalSearchScope scope) {
        FqName packageFQN = new FqName(psiPackage.getQualifiedName());

        Collection<FqName> subpackages = kotlinAsJavaSupport.getSubPackages(packageFQN, scope);

        Collection<PsiPackage> answer = Collections2.transform(subpackages, input -> new KtLightPackage(psiManager, input, scope));

        return answer.toArray(new PsiPackage[answer.size()]);
    }

    @NotNull
    @Override
    public PsiClass[] getClasses(@NotNull PsiPackage psiPackage, @NotNull GlobalSearchScope scope) {
        List<PsiClass> answer = new SmartList<>();
        FqName packageFQN = new FqName(psiPackage.getQualifiedName());

        answer.addAll(kotlinAsJavaSupport.getFacadeClassesInPackage(packageFQN, scope));

        Collection<KtClassOrObject> declarations = kotlinAsJavaSupport.findClassOrObjectDeclarationsInPackage(packageFQN, scope);
        for (KtClassOrObject declaration : declarations) {
            PsiClass aClass = toLightClass(declaration);
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
        // TODO: this does not take into account JvmPackageName annotation
        Collection<KtFile> result = kotlinAsJavaSupport.findFilesForPackage(packageFQN, scope);
        return result.toArray(new PsiFile[result.size()]);
    }

    @Override
    @Nullable
    public Condition<PsiFile> getPackageFilesFilter(@NotNull PsiPackage psiPackage, @NotNull GlobalSearchScope scope) {
        return input -> {
            if (!(input instanceof KtFile)) {
                return true;
            }
            return psiPackage.getQualifiedName().equals(((KtFile) input).getPackageFqName().asString());
        };
    }

    @NotNull
    public static Comparator<PsiElement> byClasspathComparator(@NotNull GlobalSearchScope searchScope) {
        return (o1, o2) -> {
            VirtualFile f1 = PsiUtilCore.getVirtualFile(o1);
            VirtualFile f2 = PsiUtilCore.getVirtualFile(o2);
            if (f1 == f2) return 0;
            if (f1 == null) return -1;
            if (f2 == null) return 1;
            return searchScope.compare(f2, f1);
        };
    }

    private static Collection<PsiClass> sortByClasspath(@NotNull List<PsiClass> classes, @NotNull GlobalSearchScope searchScope) {
        if (classes.size() > 1) {
            ContainerUtil.quickSort(classes, byClasspathComparator(searchScope));
        }

        return classes;
    }
}
