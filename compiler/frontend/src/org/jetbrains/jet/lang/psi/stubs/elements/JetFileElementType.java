/*
 * Copyright 2010-2013 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.jet.lang.psi.stubs.elements;

import com.intellij.lang.*;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.StubBuilder;
import com.intellij.psi.stubs.IndexSink;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.stubs.StubInputStream;
import com.intellij.psi.stubs.StubOutputStream;
import com.intellij.psi.tree.IStubFileElementType;
import com.intellij.util.io.StringRef;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.parsing.JetParser;
import org.jetbrains.jet.lang.psi.stubs.PsiJetFileStub;
import org.jetbrains.jet.lang.psi.stubs.impl.PsiJetFileStubImpl;
import org.jetbrains.jet.plugin.JetLanguage;

import java.io.IOException;

public class JetFileElementType extends IStubFileElementType<PsiJetFileStub> {
    public static final int STUB_VERSION = 23;

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
        dataStream.writeBoolean(stub.isScript());
    }

    @Override
    public PsiJetFileStub deserialize(final StubInputStream dataStream, final StubElement parentStub) throws IOException {
        StringRef packName = dataStream.readName();
        boolean isScript = dataStream.readBoolean();
        return new PsiJetFileStubImpl(null, packName, isScript);
    }

    @Override
    protected ASTNode doParseContents(@NotNull final ASTNode chameleon, @NotNull final PsiElement psi) {
        final Project project = psi.getProject();
        Language languageForParser = getLanguageForParser(psi);
        final PsiBuilder builder = PsiBuilderFactory.getInstance().createBuilder(project, chameleon, null, languageForParser, chameleon.getChars());
        final JetParser parser = (JetParser) LanguageParserDefinitions.INSTANCE.forLanguage(languageForParser).createParser(project);
        return parser.parse(this, builder, psi.getContainingFile()).getFirstChildNode();
    }


    @Override
    public void indexStub(final PsiJetFileStub stub, final IndexSink sink) {
        StubIndexServiceFactory.getInstance().indexFile(stub, sink);
    }
}
