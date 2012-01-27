package org.jetbrains.jet.lang.psi.stubs.elements;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.StubBuilder;
import com.intellij.psi.stubs.IndexSink;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.stubs.StubInputStream;
import com.intellij.psi.stubs.StubOutputStream;
import com.intellij.psi.tree.IStubFileElementType;
import com.intellij.util.io.StringRef;
import org.jetbrains.jet.lang.psi.stubs.PsiJetFileStub;
import org.jetbrains.jet.lang.psi.stubs.impl.PsiJetFileStubImpl;
import org.jetbrains.jet.plugin.JetLanguage;

import java.io.IOException;

/**
 * @author Nikolay Krasko
 */
public class JetFileElementType extends IStubFileElementType<PsiJetFileStub> {
    public static final int STUB_VERSION = 1;

    public JetFileElementType() {
        super("jet.FILE", JetLanguage.INSTANCE);
    }

    @Override
    public StubBuilder getBuilder() {
        return new JetFileStubBuilder();
    }

    @Override
    public int getStubVersion() {
        return STUB_VERSION;
    }

    @Override
    public boolean shouldBuildStubFor(final VirtualFile file) {
        // TODO:
        return true;

//        final VirtualFile dir = file.getParent();
//        return dir == null || dir.getUserData(LanguageLevel.KEY) != null;
    }

    @Override
    public ASTNode createNode(final CharSequence text) {
        // TODO (stub):
        return super.createNode(text);
    }

    @Override
    public String getExternalId() {
        return "jet.FILE";
    }

    @Override
    public void serialize(final PsiJetFileStub stub, final StubOutputStream dataStream)
            throws IOException {
        dataStream.writeName(stub.getPackageName());
    }

    @Override
    public PsiJetFileStub deserialize(final StubInputStream dataStream, final StubElement parentStub) throws IOException {
        StringRef packName = dataStream.readName();
        return new PsiJetFileStubImpl(null, packName);
    }

    @Override
    public void indexStub(final PsiJetFileStub stub, final IndexSink sink) {
        // Don't index file
    }
}
