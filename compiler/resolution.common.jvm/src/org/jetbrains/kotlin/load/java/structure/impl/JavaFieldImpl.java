/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.load.java.structure.impl;

import com.intellij.psi.PsiEnumConstant;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiVariable;
import com.intellij.psi.util.PsiUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.load.java.structure.JavaField;
import org.jetbrains.kotlin.load.java.structure.JavaType;
import org.jetbrains.kotlin.load.java.structure.impl.source.JavaElementPsiSource;
import org.jetbrains.kotlin.load.java.structure.impl.source.JavaElementSourceFactory;

public class JavaFieldImpl extends JavaMemberImpl<PsiField> implements JavaField {
    public JavaFieldImpl(@NotNull JavaElementPsiSource<PsiField> psiFieldSource) {
        super(psiFieldSource);
    }


    @SuppressWarnings("unused") // used in KSP
    public JavaFieldImpl(PsiField psiField) {
        this(JavaElementSourceFactory.getInstance(psiField.getProject()).createPsiSource(psiField));
    }

    @Override
    public boolean isEnumEntry() {
        return getPsi() instanceof PsiEnumConstant;
    }

    @Override
    @NotNull
    public JavaType getType() {
        return JavaTypeImpl.create(psiElementSource.getPsi().getType(), createVariableReturnTypeSource(psiElementSource));
    }

    @Nullable
    @Override
    public Object getInitializerValue() {
        return getPsi().computeConstantValue();
    }

    @Override
    public boolean getHasConstantNotNullInitializer() {
        // PsiUtil.isCompileTimeConstant returns false for null-initialized fields,
        // see com.intellij.psi.util.IsConstantExpressionVisitor.visitLiteralExpression()
        return PsiUtil.isCompileTimeConstant((PsiVariable) getPsi());
    }
}
