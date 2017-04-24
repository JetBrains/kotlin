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

package org.jetbrains.kotlin.asJava.elements;

import com.intellij.lang.Language;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import com.intellij.psi.search.LocalSearchScope;
import com.intellij.psi.search.SearchScope;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.asJava.builder.LightMemberOriginForDeclaration;
import org.jetbrains.kotlin.idea.KotlinLanguage;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.psi.psiUtil.KtPsiUtilKt;

import java.util.Collections;
import java.util.List;

public class KtLightParameter extends LightParameter implements KtLightDeclaration<KtParameter, PsiParameter> {
    private static String getName(PsiParameter delegate, int index) {
        String name = delegate.getName();
        return name != null ? name : "p" + index;
    }

    private final PsiModifierList modifierList;
    private final PsiParameter delegate;
    private final int index;
    private final KtLightMethod method;
    private KtLightIdentifier lightIdentifier = null;

    public KtLightParameter(PsiParameter delegate, int index, KtLightMethod method) {
        super(getName(delegate, index), delegate.getType(), method, KotlinLanguage.INSTANCE);

        this.delegate = delegate;
        this.index = index;
        this.method = method;

        if (method.getLightMemberOrigin() instanceof LightMemberOriginForDeclaration) {
            this.modifierList = new KtLightSimpleModifierList(this, Collections.emptySet());
        }
        else {
            this.modifierList = super.getModifierList();
        }
    }

    @NotNull
    @Override
    public PsiModifierList getModifierList() {
        return modifierList;
    }

    @NotNull
    @Override
    public PsiParameter getClsDelegate() {
        return delegate;
    }

    @Nullable
    @Override
    public KtParameter getKotlinOrigin() {
        KtDeclaration declaration = method.getKotlinOrigin();
        if (declaration == null) return null;

        int jetIndex = KtPsiUtilKt.isExtensionDeclaration(declaration) ? index - 1 : index;
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
        else if (declaration instanceof KtParameter) {
            return (KtParameter) declaration;
        }

        return setter != null ? setter.getParameter() : null;
    }

    @NotNull
    @Override
    public PsiElement getNavigationElement() {
        KtParameter origin = getKotlinOrigin();
        return origin != null ? origin : super.getNavigationElement();
    }

    @Override
    public boolean isValid() {
        return method.isValid();
    }

    @Override
    public PsiElement setName(@NonNls @NotNull String name) throws IncorrectOperationException {
        KtParameter origin = getKotlinOrigin();
        if (origin != null) {
            origin.setName(name);
        }
        return this;
    }

    @Override
    public PsiFile getContainingFile() {
        return method.getContainingFile();
    }

    @NotNull
    @Override
    public Language getLanguage() {
        return KotlinLanguage.INSTANCE;
    }

    @NotNull
    @Override
    public SearchScope getUseScope() {
        KtParameter origin = getKotlinOrigin();
        return origin != null ? origin.getUseScope() : new LocalSearchScope(this);
    }

    public KtLightMethod getMethod() {
        return method;
    }

    @Override
    public String getText() {
        return "";
    }

    @Override
    public TextRange getTextRange() {
        KtParameter origin = getKotlinOrigin();
        return origin != null ? origin.getTextRange() : TextRange.EMPTY_RANGE;
    }

    @Override
    public PsiIdentifier getNameIdentifier() {
        if (lightIdentifier == null) {
            lightIdentifier = new KtLightIdentifier(this, getKotlinOrigin());
        }
        return lightIdentifier;
    }

    @Override
    public PsiElement getParent() {
        return getMethod().getParameterList();
    }

    @Override
    public boolean isEquivalentTo(PsiElement another) {
        KtParameter kotlinOrigin = getKotlinOrigin();
        if (another instanceof KtLightParameter && kotlinOrigin != null) {
            KtLightParameter anotherParam = (KtLightParameter) another;
            return kotlinOrigin.equals(anotherParam.getKotlinOrigin()) && getClsDelegate().equals(anotherParam.getClsDelegate());
        }
        return super.isEquivalentTo(another);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof PsiElement && isEquivalentTo((PsiElement) obj);
    }
}
