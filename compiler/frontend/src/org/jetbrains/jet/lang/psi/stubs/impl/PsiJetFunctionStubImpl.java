package org.jetbrains.jet.lang.psi.stubs.impl;

import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.stubs.StubBase;
import com.intellij.psi.stubs.StubElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetFunction;
import org.jetbrains.jet.lang.psi.stubs.PsiJetFunctionStub;

/**
 * @author Nikolay Krasko
 */
public class PsiJetFunctionStubImpl extends StubBase<JetFunction> implements PsiJetFunctionStub<JetFunction> {
    protected PsiJetFunctionStubImpl(StubElement parent, IStubElementType elementType) {
        super(parent, elementType);
    }

//    public PsiJetFunctionStubImpl(
//            Jet type,
//            final StubElement parent,
//            final String qualifiedName,
//            final String name) {
//
//        this(type, parent, StringRef.fromString(qualifiedName), StringRef.fromString(name));
//    }
//
//    public PsiJetFunctionStubImpl(
//            JetClassElementType type,
//            final StubElement parent,
//            final StringRef qualifiedName,
//            final StringRef name) {
//
//        super(parent, type);
//        this.qualifiedName = qualifiedName;
//        this.name = name;
//    }

    @Override
    public String getName() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean isDeclaration() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @NotNull
    @Override
    public String[] getAnnotations() {
        return new String[0];  //To change body of implemented methods use File | Settings | File Templates.
    }

    @NotNull
    @Override
    public String getReturnTypeText() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
