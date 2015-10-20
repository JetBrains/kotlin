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

package org.jetbrains.kotlin.asJava;

import com.intellij.lang.Language;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.SearchScope;
import com.intellij.util.ArrayUtil;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.idea.KotlinLanguage;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.psi.psiUtil.JetPsiUtilKt;

import java.util.List;

public class KotlinLightParameter extends LightParameter implements KotlinLightElement<KtParameter, PsiParameter> {
    private static String getName(PsiParameter delegate, int index) {
        String name = delegate.getName();
        return name != null ? name : "p" + index;
    }

    private final PsiModifierList modifierList;
    private final PsiParameter delegate;
    private final int index;
    private final KotlinLightMethod method;

    public KotlinLightParameter(final PsiParameter delegate, int index, KotlinLightMethod method) {
        super(getName(delegate, index), delegate.getType(), method, KotlinLanguage.INSTANCE);

        this.delegate = delegate;
        this.index = index;
        this.method = method;

        this.modifierList = new KotlinLightModifierList(method.getManager(), ArrayUtil.EMPTY_STRING_ARRAY) {
            @Override
            public PsiAnnotationOwner getDelegate() {
                return delegate.getModifierList();
            }
        };
    }

    @NotNull
    @Override
    public PsiModifierList getModifierList() {
        return modifierList;
    }

    @NotNull
    @Override
    public PsiParameter getDelegate() {
        return delegate;
    }

    @Nullable
    @Override
    public KtParameter getOrigin() {
        KtDeclaration declaration = method.getOrigin();
        if (declaration == null) return null;

        int jetIndex = JetPsiUtilKt.isExtensionDeclaration(declaration) ? index - 1 : index;
        if (jetIndex < 0) return null;

        if (declaration instanceof KtFunction) {
            List<KtParameter> paramList = ((KtFunction) declaration).getValueParameters();
            return jetIndex < paramList.size() ? paramList.get(jetIndex) : null;
        }

        if (jetIndex != 0) return null;

        KtPropertyAccessor setter = null;
        if (declaration instanceof KtPropertyAccessor) {
            KtPropertyAccessor accessor = (KtPropertyAccessor) declaration;
            setter = accessor.isSetter() ? accessor : null;
        }
        else if (declaration instanceof KtProperty) {
            setter = ((KtProperty) declaration).getSetter();
        }

        return setter != null ? setter.getParameter() : null;
    }

    @NotNull
    @Override
    public PsiElement getNavigationElement() {
        KtParameter origin = getOrigin();
        return origin != null ? origin : super.getNavigationElement();
    }

    @Override
    public PsiElement setName(@NonNls @NotNull String name) throws IncorrectOperationException {
        KtParameter origin = getOrigin();
        if (origin != null) {
            origin.setName(name);
        }
        return this;
    }

    @Override
    public PsiFile getContainingFile() {
        KtDeclaration declaration = method.getOrigin();
        return declaration != null ? declaration.getContainingFile() : super.getContainingFile();
    }

    @NotNull
    @Override
    public Language getLanguage() {
        return KotlinLanguage.INSTANCE;
    }

    @NotNull
    @Override
    public SearchScope getUseScope() {
        KtParameter origin = getOrigin();
        return origin != null ? origin.getUseScope() : GlobalSearchScope.EMPTY_SCOPE;
    }

    public KotlinLightMethod getMethod() {
        return method;
    }
}
