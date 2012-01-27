package org.jetbrains.jet.lang.psi.stubs;

import com.intellij.psi.impl.java.stubs.StubPsiFactory;
import com.intellij.psi.stubs.PsiClassHolderFileStub;
import org.jetbrains.jet.lang.psi.JetFile;

/**
 * @author Nikolay Krasko
 */
public interface PsiJetFileStub extends PsiClassHolderFileStub<JetFile> {
    String getPackageName();

    StubPsiFactory getPsiFactory();
    void setPsiFactory(StubPsiFactory factory);
}
