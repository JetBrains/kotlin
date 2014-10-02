/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

import com.intellij.lang.Language;
import com.intellij.psi.*;
import com.intellij.psi.impl.PsiClassImplUtil;
import com.intellij.psi.impl.light.AbstractLightClass;
import com.intellij.psi.impl.light.LightField;
import com.intellij.psi.impl.light.LightMethod;
import com.intellij.psi.impl.source.ClassInnerStuffCache;
import com.intellij.psi.impl.source.PsiExtensibleClass;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.scope.PsiScopeProcessor;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import kotlin.Function1;
import kotlin.KotlinPackage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.*;

import java.util.List;

public abstract class KotlinWrappingLightClass extends AbstractLightClass implements KotlinLightClass, PsiExtensibleClass {
    private final ClassInnerStuffCache myInnersCache = new ClassInnerStuffCache(this);

    protected KotlinWrappingLightClass(PsiManager manager, Language language) {
        super(manager, language);
    }

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

    @NotNull
    @Override
    public List<PsiField> getOwnFields() {
        return ContainerUtil.map(getDelegate().getFields(), new Function<PsiField, PsiField>() {
            @Override
            public PsiField fun(PsiField field) {
                JetDeclaration declaration = ClsWrapperStubPsiFactory.getOriginalDeclaration(field);
                if (declaration instanceof JetEnumEntry) {
                    assert field instanceof PsiEnumConstant : "Field delegate should be an enum constant (" + field.getName() + "):\n" +
                                                              JetPsiUtil.getElementTextWithContext(declaration);
                    return new KotlinLightEnumConstant(myManager, (JetEnumEntry) declaration, ((PsiEnumConstant) field),
                                                       KotlinWrappingLightClass.this);
                }
                if (declaration != null) {
                    return new KotlinLightFieldForDeclaration(myManager, declaration, field, KotlinWrappingLightClass.this);
                }
                return new LightField(myManager, field, KotlinWrappingLightClass.this);
            }
        });
    }

    @NotNull
    @Override
    public List<PsiMethod> getOwnMethods() {
        return KotlinPackage.map(getDelegate().getMethods(), new Function1<PsiMethod, PsiMethod>() {
            @Override
            public PsiMethod invoke(PsiMethod method) {
                JetDeclaration declaration = ClsWrapperStubPsiFactory.getOriginalDeclaration(method);

                if (declaration != null) {
                    return !isTraitFakeOverride(declaration) ?
                           new KotlinLightMethodForDeclaration(myManager, method, declaration, KotlinWrappingLightClass.this) :
                           new KotlinLightMethodForTraitFakeOverride(myManager, method, declaration, KotlinWrappingLightClass.this);
                }

                return new LightMethod(myManager, method, KotlinWrappingLightClass.this);
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
        JetClassOrObject origin = getOrigin();
        return origin == null ? null : origin.getText();
    }

    private boolean isTraitFakeOverride(@NotNull JetDeclaration originMethodDeclaration) {
        if (!(originMethodDeclaration instanceof JetNamedFunction ||
              originMethodDeclaration instanceof JetPropertyAccessor ||
              originMethodDeclaration instanceof JetProperty)) {
            return false;
        }

        JetClassOrObject parentOfMethodOrigin = PsiTreeUtil.getParentOfType(originMethodDeclaration, JetClassOrObject.class);
        JetClassOrObject thisClassDeclaration = getOrigin();

        // Method was generated from declaration in some other trait
        return (parentOfMethodOrigin != null && thisClassDeclaration != parentOfMethodOrigin && JetPsiUtil.isTrait(parentOfMethodOrigin));
    }
}
