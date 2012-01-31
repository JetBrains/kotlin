package org.jetbrains.jet.lang.psi.stubs.elements;

import com.intellij.lang.ASTNode;
import com.intellij.lang.LighterAST;
import com.intellij.lang.LighterASTNode;
import com.intellij.psi.stubs.IndexSink;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.stubs.StubInputStream;
import com.intellij.psi.stubs.StubOutputStream;
import com.intellij.util.io.StringRef;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetClass;
import org.jetbrains.jet.lang.psi.stubs.PsiJetClassStub;
import org.jetbrains.jet.lang.psi.stubs.impl.PsiJetClassStubImpl;

import java.io.IOException;

/**
 * @author Nikolay Krasko
 */
public class JetClassElementType extends JetStubElementType<PsiJetClassStub, JetClass> {

    public JetClassElementType(@NotNull @NonNls String debugName) {
        super(debugName);
    }

    @Override
    public PsiJetClassStub createStub(LighterAST tree, LighterASTNode node, StubElement parentStub) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public JetClass createPsi(@NotNull PsiJetClassStub stub) {
        // return getPsiFactory(stub).createClass(stub);
        return null;
    }

    @Override
    public JetClass createPsiFromAst(@NotNull ASTNode node) {
        return new JetClass(node);
    }

    @Override
    public PsiJetClassStub createStub(@NotNull JetClass psi, StubElement parentStub) {
        return new PsiJetClassStubImpl(JetStubElementTypes.CLASS, parentStub, psi.getName(), psi.getName());
    }

    @Override
    public void serialize(PsiJetClassStub stub, StubOutputStream dataStream) throws IOException {
        dataStream.writeName(stub.getName());
        dataStream.writeName(stub.getQualifiedName());
    }

    @Override
    public PsiJetClassStub deserialize(StubInputStream dataStream, StubElement parentStub) throws IOException {
        final StringRef name = dataStream.readName();
        final StringRef qualifiedName = dataStream.readName();
        
        final JetClassElementType type = JetStubElementTypes.CLASS;
        final PsiJetClassStubImpl classStub = new PsiJetClassStubImpl(type, parentStub, qualifiedName, name);

        return classStub;
    }

    @Override
    public void indexStub(PsiJetClassStub stub, IndexSink sink) {
        StubIndexServiceFactory.getInstance().indexClass(stub, sink);
    }
}
