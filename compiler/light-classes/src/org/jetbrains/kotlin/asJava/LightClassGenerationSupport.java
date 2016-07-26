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

package org.jetbrains.kotlin.asJava;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.asJava.builder.LightClassConstructionContext;
import org.jetbrains.kotlin.asJava.classes.KtLightClass;
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.psi.KtClassOrObject;
import org.jetbrains.kotlin.psi.KtDeclaration;
import org.jetbrains.kotlin.psi.KtElement;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.resolve.BindingContext;

import java.util.Collection;

public abstract class LightClassGenerationSupport {

    @NotNull
    public static LightClassGenerationSupport getInstance(@NotNull Project project) {
        return ServiceManager.getService(project, LightClassGenerationSupport.class);
    }

    @NotNull
    public abstract LightClassConstructionContext getContextForClassOrObject(@NotNull KtClassOrObject classOrObject);

    @NotNull
    public abstract Collection<KtClassOrObject> findClassOrObjectDeclarations(@NotNull FqName fqName, @NotNull GlobalSearchScope searchScope);

    /*
     * Finds files whose package declaration is exactly {@code fqName}. For example, if a file declares
     *     package a.b.c
     * it will not be returned for fqName "a.b"
     *
     * If the resulting collection is empty, it means that this package has not other declarations than sub-packages
     */
    @NotNull
    public abstract Collection<KtFile> findFilesForPackage(@NotNull FqName fqName, @NotNull GlobalSearchScope searchScope);

    // Returns only immediately declared classes/objects, package classes are not included (they have no declarations)
    @NotNull
    public abstract Collection<KtClassOrObject> findClassOrObjectDeclarationsInPackage(
            @NotNull FqName packageFqName,
            @NotNull GlobalSearchScope searchScope
    );

    public abstract boolean packageExists(@NotNull FqName fqName, @NotNull GlobalSearchScope scope);

    @NotNull
    public abstract Collection<FqName> getSubPackages(@NotNull FqName fqn, @NotNull GlobalSearchScope scope);

    @Nullable
    public abstract KtLightClass getLightClass(@NotNull KtClassOrObject classOrObject);

    @Nullable
    public abstract DeclarationDescriptor resolveToDescriptor(@NotNull KtDeclaration declaration);

    @NotNull
    public abstract BindingContext analyze(@NotNull KtElement element);

    @NotNull
    public abstract Collection<PsiClass> getFacadeClasses(@NotNull FqName facadeFqName, @NotNull GlobalSearchScope scope);

    @NotNull
    public abstract Collection<PsiClass> getMultifilePartClasses(@NotNull FqName partFqName, @NotNull GlobalSearchScope scope);

    @NotNull
    public abstract Collection<PsiClass> getFacadeClassesInPackage(@NotNull FqName packageFqName, @NotNull GlobalSearchScope scope);

    @NotNull
    public abstract Collection<String> getFacadeNames(@NotNull FqName packageFqName, @NotNull GlobalSearchScope scope);

    @NotNull
    public abstract Collection<KtFile> findFilesForFacade(@NotNull FqName facadeFqName, @NotNull GlobalSearchScope scope);

    @NotNull
    public abstract LightClassConstructionContext getContextForFacade(@NotNull Collection<KtFile> files);
}
