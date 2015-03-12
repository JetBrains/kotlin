/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.psi.stubs.elements;

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
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.idea.JetLanguage;
import org.jetbrains.kotlin.parsing.JetParser;
import org.jetbrains.kotlin.psi.stubs.KotlinFileStub;
import org.jetbrains.kotlin.psi.stubs.impl.KotlinFileStubImpl;

import java.io.IOException;

public class JetFileElementType extends IStubFileElementType<KotlinFileStub> {
    public static final int STUB_VERSION = 39;

    private static final String NAME = "kotlin.FILE";

    public JetFileElementType() {
        super(NAME, JetLanguage.INSTANCE);
    }

    protected JetFileElementType(@NonNls String debugName) {
        super(debugName, JetLanguage.INSTANCE);
    }

    @Override
    public StubBuilder getBuilder() {
        return new JetFileStubBuilder();
    }

    @Override
    public int getStubVersion() {
        return STUB_VERSION;
    }

    @NotNull
    @Override
    public String getExternalId() {
        return NAME;
    }

    @Override
    public void serialize(@NotNull KotlinFileStub stub, @NotNull StubOutputStream dataStream)
            throws IOException {
        dataStream.writeName(stub.getPackageFqName().asString());
        dataStream.writeBoolean(stub.isScript());
    }

    @NotNull
    @Override
    public KotlinFileStub deserialize(@NotNull StubInputStream dataStream, StubElement parentStub) throws IOException {
        StringRef packageFqNameAsString = dataStream.readName();
        boolean isScript = dataStream.readBoolean();
        return new KotlinFileStubImpl(null, packageFqNameAsString, isScript);
    }

    @Override
    protected ASTNode doParseContents(@NotNull ASTNode chameleon, @NotNull PsiElement psi) {
        Project project = psi.getProject();
        Language languageForParser = getLanguageForParser(psi);
        PsiBuilder builder = PsiBuilderFactory.getInstance().createBuilder(project, chameleon, null, languageForParser, chameleon.getChars());
        JetParser parser = (JetParser) LanguageParserDefinitions.INSTANCE.forLanguage(languageForParser).createParser(project);
        return parser.parse(this, builder, psi.getContainingFile()).getFirstChildNode();
    }

    @Override
    public void indexStub(@NotNull KotlinFileStub stub, @NotNull IndexSink sink) {
        StubIndexServiceFactory.getInstance().indexFile(stub, sink);
    }
}
