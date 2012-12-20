/*
 * Copyright 2010-2012 JetBrains s.r.o.
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
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetNamespaceHeader;
import org.jetbrains.jet.lang.psi.JetPsiFactory;
import org.jetbrains.jet.lang.resolve.java.JetJavaMirrorMarker;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;

import javax.swing.*;
import java.util.Collection;

public class KotlinLightClassForPackage extends KotlinLightClassForPackageBase implements JetJavaMirrorMarker {

    private final FqName fqName;
    private final Collection<JetFile> files;
    private final int hashCode;
    private CachedValue<PsiClass> delegate;

    public KotlinLightClassForPackage(@NotNull PsiManager manager, @NotNull FqName fqName, @NotNull Collection<JetFile> files) {
        super(manager);
        this.fqName = fqName;
        assert !files.isEmpty() : "No files for package " + fqName;
        this.files = Sets.newHashSet(files); // needed for hashCode
        this.hashCode = computeHashCode();
        KotlinLightClassForPackageProvider stubProvider = new KotlinLightClassForPackageProvider(manager.getProject(), fqName, files);
        this.delegate = CachedValuesManager.getManager(getProject()).createCachedValue(stubProvider);
    }

    @Nullable
    @Override
    public String getName() {
        return fqName.shortName().getName();
    }

    @Nullable
    @Override
    public String getQualifiedName() {
        return fqName.getFqName();
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
        return new KotlinLightClassForPackage(getManager(), fqName, files);
    }

    @NotNull
    @Override
    public PsiClass getDelegate() {
        return delegate.getValue();
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
    public PsiElement setName(@NonNls @NotNull String name) throws IncorrectOperationException {
        for (JetFile file : files) {
            JetNamespaceHeader header = file.getNamespaceHeader();
            assert header != null : "Cannot rename a package of a script";
            String newHeaderText = "package " + fqName.parent().child(Name.identifier(name)).toString();
            JetNamespaceHeader newHeader = JetPsiFactory.createFile(getProject(), newHeaderText).getNamespaceHeader();
            assert newHeader != null;
            header.replace(newHeader);
        }
        // TODO: some other files may now belong to the same package
        return this;
    }

    @Override
    public ItemPresentation getPresentation() {
        return ItemPresentationProviders.getItemPresentation(this);
    }

    @Override
    public Icon getElementIcon(final int flags) {
        throw new UnsupportedOperationException("This should be done byt JetIconProvider");
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    private int computeHashCode() {
        int result = getManager().hashCode();
        result = 31 * result + files.hashCode();
        result = 31 * result + fqName.hashCode();
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
        if (!fqName.equals(lightClass.fqName)) return false;

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
