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

package org.jetbrains.kotlin.asJava.classes;

import com.intellij.lang.Language;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.light.AbstractLightClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.asJava.elements.KtLightAbstractAnnotation;
import org.jetbrains.kotlin.asJava.finder.JavaElementFinder;
import org.jetbrains.kotlin.idea.KotlinLanguage;
import org.jetbrains.kotlin.load.java.structure.LightClassOriginKind;
import org.jetbrains.kotlin.psi.KtClassOrObject;
import org.jetbrains.kotlin.psi.KtFile;

import java.util.List;

/**
 * This class serves as a workaround for usages of {@link JavaElementFinder#findClasses} which eventually only need names of files
 * containing the class. When queried for a package class (e.g. test/TestPackage), {@code findClasses} along with a
 * {@link KtLightClassForFacade} would also return multiple instances of this class for each file present in the package. The client
 * code can make use of every file in the package then, since {@code getContainingFile} of these instances will represent the whole package.
 * <p/>
 * See {@link LineBreakpoint#findClassCandidatesInSourceContent} for the primary usage this was introduced
 */
public class FakeLightClassForFileOfPackage extends AbstractLightClass implements KtLightClass {
    private final KtLightClassForFacade delegate;
    private final KtFile file;

    public FakeLightClassForFileOfPackage(@NotNull KtLightClassForFacade delegate, @NotNull KtFile file) {
        super(delegate.getManager());
        this.delegate = delegate;
        this.file = file;
    }

    @NotNull
    @Override
    public PsiClass getClsDelegate() {
        return delegate;
    }

    @Nullable
    @Override
    public KtClassOrObject getKotlinOrigin() {
        return null;
    }

    @Override
    public PsiFile getContainingFile() {
        return file;
    }

    @Override
    public boolean isValid() {
        // This is intentionally false to prevent using this as a real class
        return false;
    }

    @NotNull
    @Override
    public PsiClass getDelegate() {
        return delegate;
    }

    @NotNull
    @Override
    public PsiElement copy() {
        return new FakeLightClassForFileOfPackage(delegate, file);
    }

    @Override
    public String getText() {
        return null;
    }

    @NotNull
    @Override
    public Language getLanguage() {
        return KotlinLanguage.INSTANCE;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof FakeLightClassForFileOfPackage)) return false;

        FakeLightClassForFileOfPackage other = (FakeLightClassForFileOfPackage) obj;
        return file == other.file && delegate.equals(other.delegate);
    }

    @Override
    public int hashCode() {
        return file.hashCode() * 31 + delegate.hashCode();
    }

    @NotNull
    @Override
    public LightClassOriginKind getOriginKind() {
        return LightClassOriginKind.SOURCE;
    }

    @Nullable
    @Override
    public List<KtLightAbstractAnnotation> getGivenAnnotations() {
        return null;
    }
}
