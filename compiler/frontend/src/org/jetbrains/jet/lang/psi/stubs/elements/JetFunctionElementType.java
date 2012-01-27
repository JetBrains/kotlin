package org.jetbrains.jet.lang.psi.stubs.elements;

import com.intellij.lang.ASTNode;
import com.intellij.lang.LighterAST;
import com.intellij.lang.LighterASTNode;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.stubs.StubInputStream;
import com.intellij.psi.stubs.StubOutputStream;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetFunction;
import org.jetbrains.jet.lang.psi.JetNamedFunction;
import org.jetbrains.jet.lang.psi.stubs.PsiJetFunctionStub;
import org.jetbrains.jet.plugin.JetLanguage;

import java.io.IOException;

/**
 * @author Nikolay Krasko
 */
public abstract class JetFunctionElementType extends JetStubElementType<PsiJetFunctionStub, JetFunction> {

    public JetFunctionElementType(@NotNull @NonNls String debugName) {
        super(debugName, JetLanguage.INSTANCE);
    }

    @Override
    public JetFunction createPsiFromAst(@NotNull ASTNode node) {
        return new JetNamedFunction(node);
    }

    @Override
    public PsiJetFunctionStub createStub(LighterAST tree, LighterASTNode node, StubElement parentStub) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public JetFunction createPsi(@NotNull PsiJetFunctionStub stub) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public PsiJetFunctionStub createStub(@NotNull JetFunction psi, StubElement parentStub) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void serialize(PsiJetFunctionStub stub, StubOutputStream dataStream) throws IOException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public PsiJetFunctionStub deserialize(StubInputStream dataStream, StubElement parentStub) throws IOException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
