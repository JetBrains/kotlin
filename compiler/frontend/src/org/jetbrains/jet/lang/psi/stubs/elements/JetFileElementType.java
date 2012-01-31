package org.jetbrains.jet.lang.psi.stubs.elements;

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
