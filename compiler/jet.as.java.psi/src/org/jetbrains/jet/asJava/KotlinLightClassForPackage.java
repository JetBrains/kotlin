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
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiManager;
import com.intellij.psi.impl.java.stubs.PsiJavaFileStub;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValuesManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.java.JetJavaMirrorMarker;
import org.jetbrains.jet.lang.resolve.java.PackageClassUtils;
import org.jetbrains.jet.lang.resolve.name.FqName;

import javax.swing.*;
import java.util.Collection;

public class KotlinLightClassForPackage extends KotlinLightClassForPackageBase implements KotlinLightClass, JetJavaMirrorMarker {
    @Nullable
    public static KotlinLightClassForPackage create(@NotNull PsiManager manager, @NotNull FqName qualifiedName, @NotNull Collection<JetFile> files) {
        for (JetFile file : files) {
            if (LightClassUtil.belongsToKotlinBuiltIns(file)) return null;
        }
        return new KotlinLightClassForPackage(manager, qualifiedName, files);
    }

    private final FqName packageFqName;
    private final FqName packageClassFqName; // derived from packageFqName
    private final Collection<JetFile> files;
    private final int hashCode;
    private final CachedValue<PsiJavaFileStub> javaFileStub;

    private KotlinLightClassForPackage(@NotNull PsiManager manager, @NotNull FqName packageFqName, @NotNull Collection<JetFile> files) {
        super(manager);
        this.packageFqName = packageFqName;
        this.packageClassFqName = PackageClassUtils.getPackageClassFqName(packageFqName);
        assert !files.isEmpty() : "No files for package " + packageFqName;
        this.files = Sets.newHashSet(files); // needed for hashCode
        this.hashCode = computeHashCode();
        KotlinJavaFileStubProvider stubProvider = KotlinJavaFileStubProvider.createForPackageClass(getProject(), packageFqName, files);
        this.javaFileStub = CachedValuesManager.getManager(getProject()).createCachedValue(stubProvider, /*trackValue = */false);
    }

    @NotNull
    @Override
    public FqName getFqName() {
        return packageClassFqName;
    }

    @Nullable
    @Override
    public String getName() {
        return packageClassFqName.shortName().getName();
    }

    @Nullable
    @Override
    public String getQualifiedName() {
        return packageClassFqName.getFqName();
    }

    @Override
    public boolean isValid() {
        return allValid(files);
    }

    private static boolean allValid(Collection<JetFile> files) {
        for (JetFile file : files) {
            if (!file.isValid()) return false;
        }
        return true;
    }

    @NotNull
    @Override
    public PsiElement copy() {
        return new KotlinLightClassForPackage(getManager(), packageFqName, files);
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
