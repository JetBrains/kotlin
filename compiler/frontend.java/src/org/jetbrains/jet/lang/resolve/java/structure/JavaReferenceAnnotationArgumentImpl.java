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

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiEnumConstant;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiReferenceExpression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.resolve.name.Name;

public class JavaReferenceAnnotationArgumentImpl extends JavaAnnotationArgumentImpl implements JavaReferenceAnnotationArgument {
    protected JavaReferenceAnnotationArgumentImpl(@NotNull PsiReferenceExpression psiReferenceExpression, @Nullable Name name) {
        super(psiReferenceExpression, name);
    }

    @NotNull
    @Override
    public PsiReferenceExpression getPsi() {
        return (PsiReferenceExpression) super.getPsi();
    }

    @Override
    @Nullable
    public JavaElement resolve() {
        PsiReferenceExpression expression = getPsi();
        PsiElement element = expression.resolve();
        if (element instanceof PsiEnumConstant) {
            return new JavaFieldImpl((PsiField) element);
        }
        // TODO: other types of references
        return null;
    }
}
