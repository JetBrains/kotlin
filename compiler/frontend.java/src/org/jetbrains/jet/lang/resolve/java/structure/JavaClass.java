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

import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiModifierList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class JavaClass {
    private final PsiClass psiClass;

    public JavaClass(@NotNull PsiClass psiClass) {
        this.psiClass = psiClass;
    }

    @NotNull
    public PsiClass getPsiClass() {
        return psiClass;
    }

    @NotNull
    /* package */ static Collection<JavaClass> javaClassesFromPsi(@NotNull PsiClass[] classes) {
        List<JavaClass> result = new ArrayList<JavaClass>(classes.length);
        for (PsiClass psiClass : classes) {
            result.add(new JavaClass(psiClass));
        }
        return result;
    }

    @NotNull
    public Collection<JavaClass> getInnerClasses() {
        return javaClassesFromPsi(psiClass.getInnerClasses());
    }

    @Nullable
    public String getFqName() {
        return psiClass.getQualifiedName();
    }

    @Nullable
    public JavaAnnotation findAnnotation(@NotNull String fqName) {
        PsiModifierList modifierList = psiClass.getModifierList();
        if (modifierList != null) {
            PsiAnnotation annotation = modifierList.findAnnotation(fqName);
            if (annotation != null) {
                return new JavaAnnotation(annotation);
            }
        }
        return null;
    }

    @NotNull
    public String getName() {
        return psiClass.getName();
    }
}
