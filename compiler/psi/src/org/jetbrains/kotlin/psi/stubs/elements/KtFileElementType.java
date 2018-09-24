/*
 * Copyright 2010-2016 JetBrains s.r.o.
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
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.idea.KotlinLanguage;
import org.jetbrains.kotlin.parsing.KotlinParser;
import org.jetbrains.kotlin.psi.stubs.KotlinFileStub;
import org.jetbrains.kotlin.psi.stubs.KotlinStubVersions;

import java.io.IOException;

public class KtFileElementType extends IStubFileElementType<KotlinFileStub> {
    private static final String NAME = "kotlin.FILE";

    public KtFileElementType() {
        super(NAME, KotlinLanguage.INSTANCE);
    }

    protected KtFileElementType(@NonNls String debugName) {
        super(debugName, KotlinLanguage.INSTANCE);
    }

    @Override
    public StubBuilder getBuilder() {
        return new KtFileStubBuilder();
    }

    @Override
    public int getStubVersion() {
        return KotlinStubVersions.SOURCE_STUB_VERSION;
    }

    @NotNull
    @Override
    public String getExternalId() {
        return NAME;
    }

    @Override
    public void serialize(@NotNull KotlinFileStub stub, @NotNull StubOutputStream dataStream)
            throws IOException {
        StubIndexService.getInstance().serializeFileStub(stub, dataStream);
    }

    @NotNull
    @Override
    public KotlinFileStub deserialize(@NotNull StubInputStream dataStream, StubElement parentStub) throws IOException {
        return StubIndexService.getInstance().deserializeFileStub(dataStream);
    }

    @Override
    protected ASTNode doParseContents(@NotNull ASTNode chameleon, @NotNull PsiElement psi) {
        Project project = psi.getProject();
        Language languageForParser = getLanguageForParser(psi);
        PsiBuilder builder = PsiBuilderFactory.getInstance().createBuilder(project, chameleon, null, languageForParser, chameleon.getChars());
        KotlinParser parser = (KotlinParser) LanguageParserDefinitions.INSTANCE.forLanguage(languageForParser).createParser(project);
        return parser.parse(this, builder, psi.getContainingFile()).getFirstChildNode();
    }

    @Override
    public void indexStub(@NotNull KotlinFileStub stub, @NotNull IndexSink sink) {
        StubIndexService.getInstance().indexFile(stub, sink);
    }
}
