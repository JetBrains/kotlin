/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava.builder;

import com.intellij.openapi.util.Key;
import com.intellij.psi.*;
import com.intellij.psi.impl.compiled.ClsClassImpl;
import com.intellij.psi.impl.compiled.ClsEnumConstantImpl;
import com.intellij.psi.impl.compiled.ClsFieldImpl;
import com.intellij.psi.impl.java.stubs.*;
import com.intellij.psi.stubs.StubBase;
import com.intellij.psi.stubs.StubElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ClsWrapperStubPsiFactory extends StubPsiFactory {
    public static final Key<LightElementOrigin> ORIGIN = Key.create("ORIGIN");
    public static final ClsWrapperStubPsiFactory INSTANCE = new ClsWrapperStubPsiFactory();

    private final StubPsiFactory delegate = new ClsStubPsiFactory();

    private ClsWrapperStubPsiFactory() { }

    @Override
    public PsiClass createClass(@NotNull PsiClassStub stub) {
        PsiElement origin = getOriginalElement(stub);
        return new ClsClassImpl(stub) {
            @NotNull
            @Override
            public PsiElement getNavigationElement() {
                if (origin != null) return origin;

                return super.getNavigationElement();
            }

            @Nullable
            @Override
            public PsiClass getSourceMirrorClass() {
                return null;
            }
        };
    }

    @Nullable
    public static PsiElement getOriginalElement(@NotNull StubElement stub) {
        LightElementOrigin origin = ((StubBase) stub).getUserData(ORIGIN);
        return origin != null ? origin.getOriginalElement() : null;
    }

    @Override
    public PsiAnnotation createAnnotation(PsiAnnotationStub stub) {
        return delegate.createAnnotation(stub);
    }

    @Override
    public PsiClassInitializer createClassInitializer(PsiClassInitializerStub stub) {
        return delegate.createClassInitializer(stub);
    }

    @Override
    public PsiReferenceList createClassReferenceList(PsiClassReferenceListStub stub) {
        return delegate.createClassReferenceList(stub);
    }

    @Override
    public PsiField createField(PsiFieldStub stub) {
        PsiElement origin = getOriginalElement(stub);
        if (origin == null) return delegate.createField(stub);
        if (stub.isEnumConstant()) {
            return new ClsEnumConstantImpl(stub) {
                @NotNull
                @Override
                public PsiElement getNavigationElement() {
                    return origin;
                }
            };
        }
        else {
            return new ClsFieldImpl(stub) {
                @NotNull
                @Override
                public PsiElement getNavigationElement() {
                    return origin;
                }
            };
        }
    }

    @Override
    public PsiImportList createImportList(PsiImportListStub stub) {
        return delegate.createImportList(stub);
    }

    @Override
    public PsiImportStatementBase createImportStatement(PsiImportStatementStub stub) {
        return delegate.createImportStatement(stub);
    }

    @Override
    public PsiMethod createMethod(PsiMethodStub stub) {
        return delegate.createMethod(stub);
    }

    @Override
    public PsiModifierList createModifierList(PsiModifierListStub stub) {
        return delegate.createModifierList(stub);
    }

    @Override
    public PsiParameter createParameter(PsiParameterStub stub) {
        return delegate.createParameter(stub);
    }

    @Override
    public PsiParameterList createParameterList(PsiParameterListStub stub) {
        return delegate.createParameterList(stub);
    }

    @Override
    public PsiTypeParameter createTypeParameter(PsiTypeParameterStub stub) {
        return delegate.createTypeParameter(stub);
    }

    @Override
    public PsiTypeParameterList createTypeParameterList(PsiTypeParameterListStub stub) {
        return delegate.createTypeParameterList(stub);
    }

    @Override
    public PsiAnnotationParameterList createAnnotationParameterList(PsiAnnotationParameterListStub stub) {
        return delegate.createAnnotationParameterList(stub);
    }

    @Override
    public PsiNameValuePair createNameValuePair(PsiNameValuePairStub stub) {
        return delegate.createNameValuePair(stub);
    }
}
