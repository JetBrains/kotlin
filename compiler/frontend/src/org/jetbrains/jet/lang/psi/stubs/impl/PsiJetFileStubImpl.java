package org.jetbrains.jet.lang.psi.stubs.impl;

import com.intellij.psi.PsiClass;
import com.intellij.psi.impl.java.stubs.StubPsiFactory;
import com.intellij.psi.stubs.PsiFileStubImpl;
import com.intellij.util.io.StringRef;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.stubs.PsiJetFileStub;

/**
 * @author Nikolay Krasko
 */
public class PsiJetFileStubImpl extends PsiFileStubImpl<JetFile> implements PsiJetFileStub {

    private final StringRef packageName;
    private StubPsiFactory psiFactory;

    public PsiJetFileStubImpl(JetFile jetFile, StringRef packageName) {
        super(jetFile);

        this.packageName = packageName;
    }

    @Override
    public String getPackageName() {
        return StringRef.toString(packageName);
    }

    @Override
    public StubPsiFactory getPsiFactory() {
        return psiFactory;
    }

    @Override
    public void setPsiFactory(StubPsiFactory factory) {
        psiFactory = factory;
    }

    @Override
    public PsiClass[] getClasses() {
        return new PsiClass[0];  //To change body of implemented methods use File | Settings | File Templates.
    }
}
