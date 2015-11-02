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
import com.intellij.navigation.ItemPresentation;
import com.intellij.navigation.ItemPresentationProviders;
import com.intellij.psi.*;
import com.intellij.psi.impl.PsiClassImplUtil;
import com.intellij.psi.impl.light.AbstractLightClass;
import com.intellij.psi.impl.source.ClassInnerStuffCache;
import com.intellij.psi.impl.source.PsiExtensibleClass;
import com.intellij.psi.scope.PsiScopeProcessor;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import kotlin.ArraysKt;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.idea.KotlinLanguage;
import org.jetbrains.kotlin.psi.*;

import java.util.List;

public abstract class KtWrappingLightClass extends AbstractLightClass implements KtLightClass, PsiExtensibleClass {
    private final ClassInnerStuffCache myInnersCache = new ClassInnerStuffCache(this);

    protected KtWrappingLightClass(PsiManager manager) {
        super(manager, KotlinLanguage.INSTANCE);
    }

    @Nullable
    @Override
    public abstract KtClassOrObject getOrigin();

    @NotNull
    @Override
    public abstract PsiClass getDelegate();

    @Override
    @NotNull
    public PsiField[] getFields() {
        return myInnersCache.getFields();
    }

    @Override
    @NotNull
    public PsiMethod[] getMethods() {
        return myInnersCache.getMethods();
    }

    @Override
    @NotNull
    public PsiMethod[] getConstructors() {
        return myInnersCache.getConstructors();
    }

    @Override
    @NotNull
    public PsiClass[] getInnerClasses() {
        return myInnersCache.getInnerClasses();
    }

    @Override
    @NotNull
    public PsiField[] getAllFields() {
        return PsiClassImplUtil.getAllFields(this);
    }

    @Override
    @NotNull
    public PsiMethod[] getAllMethods() {
        return PsiClassImplUtil.getAllMethods(this);
    }

    @Override
    @NotNull
    public PsiClass[] getAllInnerClasses() {
        return PsiClassImplUtil.getAllInnerClasses(this);
    }

    @Override
    public PsiField findFieldByName(String name, boolean checkBases) {
        return myInnersCache.findFieldByName(name, checkBases);
    }

    @Override
    @NotNull
    public PsiMethod[] findMethodsByName(String name, boolean checkBases) {
        return myInnersCache.findMethodsByName(name, checkBases);
    }

    @Override
    public PsiClass findInnerClassByName(String name, boolean checkBases) {
        return myInnersCache.findInnerClassByName(name, checkBases);
    }

    /**
     * @see org.jetbrains.kotlin.codegen.binding.CodegenBinding#ENUM_ENTRY_CLASS_NEED_SUBCLASS
      */
    @NotNull
    @Override
    public List<PsiField> getOwnFields() {
        return ContainerUtil.map(getDelegate().getFields(), new Function<PsiField, PsiField>() {
            @Override
            public PsiField fun(PsiField field) {
                KtDeclaration declaration = ClsWrapperStubPsiFactory.getOriginalDeclaration(field);
                return KtLightFieldImpl.Factory.create(declaration, field, KtWrappingLightClass.this);
            }
        });
    }

    @NotNull
    @Override
    public List<PsiMethod> getOwnMethods() {
        return ArraysKt.map(getDelegate().getMethods(), new Function1<PsiMethod, PsiMethod>() {
            @Override
            public PsiMethod invoke(PsiMethod method) {
                KtDeclaration declaration = ClsWrapperStubPsiFactory.getOriginalDeclaration(method);
                if (declaration instanceof KtPropertyAccessor) {
                    declaration = PsiTreeUtil.getParentOfType(declaration, KtProperty.class);
                }

                return KtLightMethodImpl.Factory.create(method, declaration, KtWrappingLightClass.this);
            }
        });
    }

    @Override
    public boolean processDeclarations(
            @NotNull PsiScopeProcessor processor, @NotNull ResolveState state, PsiElement lastParent, @NotNull PsiElement place
    ) {
        if (isEnum()) {
            if (!PsiClassImplUtil.processDeclarationsInEnum(processor, state, myInnersCache)) return false;
        }

        return super.processDeclarations(processor, state, lastParent, place);
    }

    @Override
    public String getText() {
        KtClassOrObject origin = getOrigin();
        return origin == null ? null : origin.getText();
    }

    @NotNull
    @Override
    public Language getLanguage() {
        return KotlinLanguage.INSTANCE;
    }

    @Override
    public ItemPresentation getPresentation() {
        return ItemPresentationProviders.getItemPresentation(this);
    }

    @Override
    public abstract boolean equals(Object obj);

    @Override
    public abstract int hashCode();

    @Override
    public PsiElement getContext() {
        return getParent();
    }
}
