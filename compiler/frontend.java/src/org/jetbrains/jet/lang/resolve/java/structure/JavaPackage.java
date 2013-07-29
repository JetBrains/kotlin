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

package org.jetbrains.jet.lang.resolve.java.structure;

import com.intellij.psi.PsiPackage;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class JavaPackage {
    private final PsiPackage psiPackage;

    public JavaPackage(@NotNull PsiPackage psiPackage) {
        this.psiPackage = psiPackage;
    }

    @NotNull
    public PsiPackage getPsiPackage() {
        return psiPackage;
    }

    @NotNull
    private static Collection<JavaPackage> javaPackagesFromPsi(@NotNull PsiPackage[] packages) {
        List<JavaPackage> result = new ArrayList<JavaPackage>(packages.length);
        for (PsiPackage psiPackage : packages) {
            result.add(new JavaPackage(psiPackage));
        }
        return result;
    }

    @NotNull
    public Collection<JavaClass> getClasses() {
        return JavaClass.javaClassesFromPsi(psiPackage.getClasses());
    }

    @NotNull
    public Collection<JavaPackage> getSubPackages() {
        return javaPackagesFromPsi(psiPackage.getSubPackages());
    }

    @NotNull
    public String getFqName() {
        return psiPackage.getQualifiedName();
    }

    @NotNull
    public String getName() {
        String name = psiPackage.getName();
        return name == null ? "" : name;
    }
}
