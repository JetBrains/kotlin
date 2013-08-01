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

import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.resolve.name.Name;

public abstract class JavaAnnotationArgument extends JavaElementImpl {
    private final Name name;

    protected JavaAnnotationArgument(@NotNull PsiAnnotationMemberValue psiAnnotationMemberValue, @Nullable Name name) {
        super(psiAnnotationMemberValue);
        this.name = name;
    }

    @NotNull
    @Override
    public PsiAnnotationMemberValue getPsi() {
        return (PsiAnnotationMemberValue) super.getPsi();
    }

    @NotNull
    public static JavaAnnotationArgument create(@NotNull PsiAnnotationMemberValue argument, @Nullable Name name) {
        if (argument instanceof PsiLiteralExpression) {
            return new JavaLiteralAnnotationArgument((PsiLiteralExpression) argument, name);
        }
        else if (argument instanceof PsiReferenceExpression) {
            return new JavaReferenceAnnotationArgument((PsiReferenceExpression) argument, name);
        }
        else if (argument instanceof PsiArrayInitializerMemberValue) {
            return new JavaArrayAnnotationArgument((PsiArrayInitializerMemberValue) argument, name);
        }
        else if (argument instanceof PsiAnnotation) {
            return new JavaAnnotationAsAnnotationArgument((PsiAnnotation) argument, name);
        }
        else {
            throw new UnsupportedOperationException("Unsupported annotation argument type: " + argument);
        }
    }

    @Nullable
    public Name getName() {
        return name;
    }
}
