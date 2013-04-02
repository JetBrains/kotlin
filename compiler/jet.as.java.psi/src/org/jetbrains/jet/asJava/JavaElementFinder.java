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

package org.jetbrains.jet.asJava;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.Sets;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElementFinder;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.SmartList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.codegen.NamespaceCodegen;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.java.JavaPsiFacadeKotlinHacks;
import org.jetbrains.jet.lang.resolve.java.PackageClassUtils;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.util.QualifiedNamesUtil;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public class JavaElementFinder extends PsiElementFinder implements JavaPsiFacadeKotlinHacks.KotlinFinderMarker {

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

    public JavaElementFinder(
            @NotNull Project project,
            @NotNull LightClassGenerationSupport lightClassGenerationSupport
    ) {
        this.project = project;
        this.psiManager = PsiManager.getInstance(project);
        this.lightClassGenerationSupport = lightClassGenerationSupport;
    }

    @Override
    public PsiClass findClass(@NotNull String qualifiedName, @NotNull GlobalSearchScope scope) {
        PsiClass[] allClasses = findClasses(qualifiedName, scope);
        return allClasses.length > 0 ? allClasses[0] : null;
    }

    @NotNull
    @Override
    public PsiClass[] findClasses(@NotNull String qualifiedNameString, @NotNull GlobalSearchScope scope) {
        if (!QualifiedNamesUtil.isValidJavaFqName(qualifiedNameString)) {
            return PsiClass.EMPTY_ARRAY;
        }

        List<PsiClass> answer = new SmartList<PsiClass>();

        FqName qualifiedName = new FqName(qualifiedNameString);

        findClassesAndObjects(qualifiedName, scope, answer);

        if (PackageClassUtils.isPackageClass(qualifiedName)) {
            findPackageClass(qualifiedName.parent(), scope, answer);
        }

        return answer.toArray(new PsiClass[answer.size()]);
    }

    // Finds explicitly declared classes and objects, not package classes
    private void findClassesAndObjects(FqName qualifiedName, GlobalSearchScope scope, List<PsiClass> answer) {
        Collection<JetClassOrObject> classOrObjectDeclarations =
                lightClassGenerationSupport.findClassOrObjectDeclarations(qualifiedName, scope);

        for (JetClassOrObject declaration : classOrObjectDeclarations) {
            if (!(declaration instanceof JetEnumEntry)) {
                PsiClass lightClass = LightClassUtil.createLightClass(declaration);
                if (lightClass != null) {
                    answer.add(lightClass);
                }
            }
        }
    }

    private void findPackageClass(FqName qualifiedName, GlobalSearchScope scope, List<PsiClass> answer) {
        Collection<JetFile> filesForPackage = lightClassGenerationSupport.findFilesForPackage(qualifiedName, scope);

        if (!filesForPackage.isEmpty() && NamespaceCodegen.shouldGenerateNSClass(filesForPackage)) {
            KotlinLightClassForPackage lightClass = KotlinLightClassForPackage.create(psiManager, qualifiedName, scope, filesForPackage);
            if (lightClass != null) {
                answer.add(lightClass);

                if (filesForPackage.size() > 1) {
                    for (JetFile file : filesForPackage) {
                        FakeLightClassForFileOfPackage fakeLightClass = new FakeLightClassForFileOfPackage(psiManager, lightClass, file);
                        answer.add(fakeLightClass);
                    }
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
        if (!QualifiedNamesUtil.isValidJavaFqName(qualifiedNameString)) {
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

        findPackageClass(packageFQN, scope, answer);

        Collection<JetClassOrObject> declarations = lightClassGenerationSupport.findClassOrObjectDeclarationsInPackage(packageFQN, scope);
        for (JetClassOrObject declaration : declarations) {
            PsiClass aClass = LightClassUtil.createLightClass(declaration);
            if (aClass != null) {
                answer.add(aClass);
            }
        }

        return answer.toArray(new PsiClass[answer.size()]);
    }
}

