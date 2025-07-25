/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
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
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.parsing.KotlinParser;
import org.jetbrains.kotlin.psi.stubs.KotlinFileStub;
import org.jetbrains.kotlin.psi.stubs.KotlinStubVersions;
import org.jetbrains.kotlin.psi.stubs.impl.KotlinFileStubImpl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class KtFileElementType extends IStubFileElementType<KotlinFileStub> {
    private static final String NAME = "kotlin.FILE";

    public static final KtFileElementType INSTANCE = new KtFileElementType();

    private KtFileElementType() {
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
    public void serialize(@NotNull KotlinFileStub stub, @NotNull StubOutputStream dataStream) throws IOException {
        KotlinFileStubImpl fileStub = (KotlinFileStubImpl) stub;
        dataStream.writeName(fileStub.getPackageFqName().asString());
        dataStream.writeBoolean(fileStub.isScript());
        FqName facadeFqName = fileStub.getFacadeFqName();
        dataStream.writeName(facadeFqName != null ? facadeFqName.asString() : null);
        dataStream.writeName(fileStub.getPartSimpleName());
        List<String> facadePartNames = fileStub.getFacadePartSimpleNames();
        if (facadePartNames == null) {
            dataStream.writeInt(0);
        }
        else {
            dataStream.writeInt(facadePartNames.size());
            for (String partName : facadePartNames) {
                dataStream.writeName(partName);
            }
        }
    }

    @NotNull
    @Override
    public KotlinFileStub deserialize(@NotNull StubInputStream dataStream, StubElement parentStub) throws IOException {
        String packageFqNameAsString = dataStream.readNameString();
        if (packageFqNameAsString == null) {
            throw new IllegalStateException("Can't read package fqname from stream");
        }

        boolean isScript = dataStream.readBoolean();
        String facadeString = dataStream.readNameString();
        String partSimpleName = dataStream.readNameString();
        int numPartNames = dataStream.readInt();
        List<String> facadePartNames = new ArrayList<>();
        for (int i = 0; i < numPartNames; ++i) {
            String partNameRef = dataStream.readNameString();
            facadePartNames.add(partNameRef);
        }

        return new KotlinFileStubImpl(null, packageFqNameAsString, isScript, facadeString, partSimpleName, facadePartNames);
    }

    @Override
    protected ASTNode doParseContents(@NotNull ASTNode chameleon, @NotNull PsiElement psi) {
        Project project = psi.getProject();
        Language languageForParser = getLanguageForParser(psi);
        PsiBuilder builder = PsiBuilderFactory.getInstance().createBuilder(project, chameleon, null, languageForParser, chameleon.getChars());
        return KotlinParser.parse(builder, psi.getContainingFile()).getFirstChildNode();
    }

    @Override
    public void indexStub(@NotNull KotlinFileStub stub, @NotNull IndexSink sink) {
        StubIndexService.getInstance().indexFile(stub, sink);
    }
}
