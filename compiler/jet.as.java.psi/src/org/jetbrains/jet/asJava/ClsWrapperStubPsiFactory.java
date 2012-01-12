/*
 * @author max
 */
package org.jetbrains.jet.asJava;

import com.intellij.openapi.util.Key;
import com.intellij.psi.*;
import com.intellij.psi.impl.compiled.ClsClassImpl;
import com.intellij.psi.impl.compiled.ClsEnumConstantImpl;
import com.intellij.psi.impl.compiled.ClsFieldImpl;
import com.intellij.psi.impl.compiled.ClsMethodImpl;
import com.intellij.psi.impl.java.stubs.*;
import com.intellij.psi.stubs.StubBase;
import org.jetbrains.annotations.NotNull;

class ClsWrapperStubPsiFactory extends StubPsiFactory {
    public static final Key<PsiElement> ORIGIN_ELEMENT = Key.create("ORIGIN_ELEMENT");
    private final StubPsiFactory delegate = new ClsStubPsiFactory();
    
    @Override
    public PsiClass createClass(PsiClassStub stub) {
        final PsiElement origin = ((StubBase) stub).getUserData(ORIGIN_ELEMENT);
        if (origin == null) return delegate.createClass(stub);
        
        return new ClsClassImpl(stub) {
            @NotNull
            @Override
            public PsiElement getNavigationElement() {
                return origin;
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
        final PsiElement origin = ((StubBase) stub).getUserData(ORIGIN_ELEMENT);
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
        final PsiElement origin = ((StubBase) stub).getUserData(ORIGIN_ELEMENT);
        if (origin == null) return delegate.createMethod(stub);

        return new ClsMethodImpl(stub) {
            @NotNull
            @Override
            public PsiElement getNavigationElement() {
                return origin;
            }
        };
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

}
