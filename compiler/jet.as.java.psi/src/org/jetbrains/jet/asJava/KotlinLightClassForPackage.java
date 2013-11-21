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

import com.google.common.collect.Sets;
import com.intellij.navigation.ItemPresentation;
import com.intellij.navigation.ItemPresentationProviders;
import com.intellij.openapi.util.Comparing;
import com.intellij.psi.*;
import com.intellij.psi.impl.java.stubs.PsiJavaFileStub;
import com.intellij.psi.impl.light.LightEmptyImplementsList;
import com.intellij.psi.impl.light.LightModifierList;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValuesManager;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.java.jetAsJava.JetJavaMirrorMarker;
import org.jetbrains.jet.lang.resolve.java.PackageClassUtils;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.plugin.JetLanguage;

import javax.swing.*;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class KotlinLightClassForPackage extends KotlinWrappingLightClass implements KotlinLightClass, JetJavaMirrorMarker {
    private final FqName packageFqName;
    private final FqName packageClassFqName; // derived from packageFqName
    private final GlobalSearchScope searchScope;
    private final Collection<JetFile> files;
    private final int hashCode;
    private final CachedValue<PsiJavaFileStub> javaFileStub;
    private final PsiModifierList modifierList;
    private final LightEmptyImplementsList implementsList;

    private KotlinLightClassForPackage(
            @NotNull PsiManager manager,
            @NotNull FqName packageFqName,
            @NotNull GlobalSearchScope searchScope,
            @NotNull Collection<JetFile> files
    ) {
        super(manager, JetLanguage.INSTANCE);
        this.modifierList = new LightModifierList(manager, JetLanguage.INSTANCE, PsiModifier.PUBLIC, PsiModifier.FINAL);
        this.implementsList = new LightEmptyImplementsList(manager);
        this.packageFqName = packageFqName;
        this.packageClassFqName = PackageClassUtils.getPackageClassFqName(packageFqName);
        this.searchScope = searchScope;
        assert !files.isEmpty() : "No files for package " + packageFqName;
        this.files = Sets.newHashSet(files); // needed for hashCode
        this.hashCode = computeHashCode();
        KotlinJavaFileStubProvider stubProvider =
                KotlinJavaFileStubProvider.createForPackageClass(getProject(), packageFqName, searchScope);
        this.javaFileStub = CachedValuesManager.getManager(getProject()).createCachedValue(stubProvider, /*trackValue = */false);
    }

    @Nullable
    public static KotlinLightClassForPackage create(
            @NotNull PsiManager manager,
            @NotNull FqName qualifiedName,
            @NotNull GlobalSearchScope searchScope,
            @NotNull Collection<JetFile> files // this is redundant, but computing it multiple times is costly
    ) {
        for (JetFile file : files) {
            if (LightClassUtil.belongsToKotlinBuiltIns(file)) return null;
        }
        return new KotlinLightClassForPackage(manager, qualifiedName, searchScope, files);
    }

    private static boolean allValid(Collection<JetFile> files) {
        for (JetFile file : files) {
            if (!file.isValid()) return false;
        }
        return true;
    }

    @Nullable
    @Override
    public PsiModifierList getModifierList() {
        return modifierList;
    }

    @Override
    public boolean hasModifierProperty(@NonNls @NotNull String name) {
        return modifierList.hasModifierProperty(name);
    }

    @Override
    public boolean isDeprecated() {
        return false;
    }

    @Override
    public boolean isInterface() {
        return false;
    }

    @Override
    public boolean isAnnotationType() {
        return false;
    }

    @Override
    public boolean isEnum() {
        return false;
    }

    @Nullable
    @Override
    public PsiClass getContainingClass() {
        return null;
    }

    @Override
    public boolean hasTypeParameters() {
        return false;
    }

    @NotNull
    @Override
    public PsiTypeParameter[] getTypeParameters() {
        return PsiTypeParameter.EMPTY_ARRAY;
    }

    @Nullable
    @Override
    public PsiTypeParameterList getTypeParameterList() {
        return null;
    }

    @Nullable
    @Override
    public PsiDocComment getDocComment() {
        return null;
    }

    @Nullable
    @Override
    public PsiReferenceList getImplementsList() {
        return implementsList;
    }

    @NotNull
    @Override
    public PsiClassType[] getImplementsListTypes() {
        return PsiClassType.EMPTY_ARRAY;
    }

    @Nullable
    @Override
    public PsiReferenceList getExtendsList() {
        // TODO: Find a way to return just Object
        return super.getExtendsList();
    }

    @NotNull
    @Override
    public PsiClassType[] getExtendsListTypes() {
        // TODO see getExtendsList()
        return super.getExtendsListTypes();
    }

    @Nullable
    @Override
    public PsiClass getSuperClass() {
        // TODO see getExtendsList()
        return super.getSuperClass();
    }

    @NotNull
    @Override
    public PsiClass[] getSupers() {
        // TODO see getExtendsList()
        return super.getSupers();
    }

    @NotNull
    @Override
    public PsiClassType[] getSuperTypes() {
        // TODO see getExtendsList()
        return super.getSuperTypes();
    }

    @Override
    public PsiClass[] getInterfaces() {
        return PsiClass.EMPTY_ARRAY;
    }

    @NotNull
    @Override
    public PsiClass[] getInnerClasses() {
        return PsiClass.EMPTY_ARRAY;
    }

    @NotNull
    @Override
    public List<PsiClass> getOwnInnerClasses() {
        return Collections.emptyList();
    }

    @NotNull
    @Override
    public PsiClass[] getAllInnerClasses() {
        return PsiClass.EMPTY_ARRAY;
    }

    @NotNull
    @Override
    public PsiClassInitializer[] getInitializers() {
        return PsiClassInitializer.EMPTY_ARRAY;
    }

    @Nullable
    @Override
    public PsiClass findInnerClassByName(@NonNls String name, boolean checkBases) {
        return null;
    }

    @NotNull
    @Override
    public FqName getFqName() {
        return packageClassFqName;
    }

    @Nullable
    @Override
    public String getName() {
        return packageClassFqName.shortName().asString();
    }

    @Nullable
    @Override
    public String getQualifiedName() {
        return packageClassFqName.asString();
    }

    @Override
    public boolean isValid() {
        return allValid(files);
    }

    @NotNull
    @Override
    public PsiElement copy() {
        return new KotlinLightClassForPackage(getManager(), packageFqName, searchScope, files);
    }

    @NotNull
    @Override
    public PsiClass getDelegate() {
        PsiClass psiClass = LightClassUtil.findClass(packageClassFqName, javaFileStub.getValue());
        if (psiClass == null) {
            throw new IllegalStateException("Package class was not found " + packageFqName);
        }
        return psiClass;
    }

    @NotNull
    @Override
    public PsiElement getNavigationElement() {
        return files.iterator().next();
    }

    @Override
    public boolean isEquivalentTo(PsiElement another) {
        return another instanceof PsiClass && Comparing.equal(((PsiClass) another).getQualifiedName(), getQualifiedName());
    }

    @Override
    public ItemPresentation getPresentation() {
        return ItemPresentationProviders.getItemPresentation(this);
    }

    @Override
    public Icon getElementIcon(int flags) {
        throw new UnsupportedOperationException("This should be done byt JetIconProvider");
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    private int computeHashCode() {
        int result = getManager().hashCode();
        result = 31 * result + files.hashCode();
        result = 31 * result + packageFqName.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        KotlinLightClassForPackage lightClass = (KotlinLightClassForPackage) obj;

        if (this.hashCode != lightClass.hashCode) return false;
        if (getManager() != lightClass.getManager()) return false;
        if (!files.equals(lightClass.files)) return false;
        if (!packageFqName.equals(lightClass.packageFqName)) return false;

        return true;
    }

    @Override
    public String toString() {
        try {
            return KotlinLightClassForPackage.class.getSimpleName() + ":" + getQualifiedName();
        }
        catch (Throwable e) {
            return KotlinLightClassForPackage.class.getSimpleName() + ":" + e.toString();
        }
    }
}
