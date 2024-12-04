/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava.builder;

import com.intellij.psi.*;
import com.intellij.psi.impl.compiled.ClsClassImpl;
import com.intellij.psi.impl.java.stubs.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ClsWrapperStubPsiFactory extends StubPsiFactory {
    public static final ClsWrapperStubPsiFactory INSTANCE = new ClsWrapperStubPsiFactory();

    private final StubPsiFactory delegate = new ClsStubPsiFactory();

    private ClsWrapperStubPsiFactory() { }

    @Override
    public PsiClass createClass(@NotNull PsiClassStub stub) {
        return new ClsClassImpl(stub) {
            @Nullable
            @Override
            public PsiClass getSourceMirrorClass() {
                return null;
            }
        };
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
        return delegate.createField(stub);
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
