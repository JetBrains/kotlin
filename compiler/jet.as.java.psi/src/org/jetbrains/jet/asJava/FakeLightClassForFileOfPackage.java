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

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.java.JetJavaMirrorMarker;
import org.jetbrains.jet.lang.resolve.name.FqName;

/**
 * This class serves as a workaround for usages of {@link JavaElementFinder#findClasses} which eventually only need names of files
 * containing the class. When queried for a package class (e.g. test/TestPackage), {@code findClasses} along with a
 * {@link KotlinLightClassForPackage} would also return multiple instances of this class for each file present in the package. The client
 * code can make use of every file in the package then, since {@code getContainingFile} of these instances will represent the whole package.
 * <p/>
 * See {@link LineBreakpoint#findClassCandidatesInSourceContent} for the primary usage this was introduced
 */
/* package */ class FakeLightClassForFileOfPackage extends KotlinLightClassForPackageBase implements KotlinLightClass, JetJavaMirrorMarker {
    private final KotlinLightClassForPackage delegate;
    private final JetFile file;

    public FakeLightClassForFileOfPackage(
            @NotNull PsiManager manager, @NotNull KotlinLightClassForPackage delegate, @NotNull JetFile file
    ) {
        super(manager);
        this.delegate = delegate;
        this.file = file;
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
    public FqName getFqName() {
        return delegate.getFqName();
    }

    @NotNull
    @Override
    public PsiClass getDelegate() {
        return delegate;
    }

    @NotNull
    @Override
    public PsiElement copy() {
        return new FakeLightClassForFileOfPackage(getManager(), delegate, file);
    }
}
