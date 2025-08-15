/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.elements;

import com.intellij.psi.stubs.IndexSink;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.stubs.StubInputStream;
import com.intellij.psi.stubs.StubOutputStream;
import com.intellij.util.io.StringRef;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.psi.KtNamedFunction;
import org.jetbrains.kotlin.psi.psiUtil.KtPsiUtilKt;
import org.jetbrains.kotlin.psi.stubs.KotlinFunctionStub;
import org.jetbrains.kotlin.psi.stubs.impl.KotlinFunctionStubImpl;
import org.jetbrains.kotlin.psi.stubs.impl.KotlinStubOrigin;

import java.io.IOException;

public class KtFunctionElementType extends KtStubElementType<KotlinFunctionStubImpl, KtNamedFunction> {

    private static final String NAME = "kotlin.FUNCTION";

    public KtFunctionElementType(@NotNull @NonNls String debugName) {
        super(debugName, KtNamedFunction.class, KotlinFunctionStub.class);
    }

    @NotNull
    @Override
    public KotlinFunctionStubImpl createStub(@NotNull KtNamedFunction psi, @NotNull StubElement parentStub) {
        boolean isTopLevel = psi.getParent() instanceof KtFile;
        boolean isExtension = psi.getReceiverTypeReference() != null;
        FqName fqName = KtPsiUtilKt.safeFqNameForLazyResolve(psi);
        boolean hasNoExpressionBody = psi.hasBlockBody();
        boolean hasBody = psi.hasBody();
        return new KotlinFunctionStubImpl(
                (StubElement<?>) parentStub, StringRef.fromString(psi.getName()), isTopLevel, fqName,
                isExtension, hasNoExpressionBody, hasBody, psi.hasTypeParameterListBeforeFunctionName(),
                psi.mayHaveContract(),
                /* contract = */ null,
                /* origin = */ null
        );
    }

    @Override
    public void serialize(@NotNull KotlinFunctionStubImpl stub, @NotNull StubOutputStream dataStream) throws IOException {
        dataStream.writeName(stub.getName());
        dataStream.writeBoolean(stub.isTopLevel());

        FqName fqName = stub.getFqName();
        dataStream.writeName(fqName != null ? fqName.asString() : null);

        dataStream.writeBoolean(stub.isExtension());
        dataStream.writeBoolean(stub.getHasNoExpressionBody());
        dataStream.writeBoolean(stub.getHasBody());
        dataStream.writeBoolean(stub.getHasTypeParameterListBeforeFunctionName());
        boolean haveContract = stub.getMayHaveContract();
        dataStream.writeBoolean(haveContract);

        if (haveContract) {
            stub.serializeContract(dataStream);
        }

        KotlinStubOrigin.serialize(stub.getOrigin(), dataStream);
    }

    @NotNull
    @Override
    public KotlinFunctionStubImpl deserialize(@NotNull StubInputStream dataStream, StubElement parentStub) throws IOException {
        StringRef name = dataStream.readName();
        boolean isTopLevel = dataStream.readBoolean();

        StringRef fqNameAsString = dataStream.readName();
        FqName fqName = fqNameAsString != null ? new FqName(fqNameAsString.toString()) : null;

        boolean isExtension = dataStream.readBoolean();
        boolean hasNoExpressionBody = dataStream.readBoolean();
        boolean hasBody = dataStream.readBoolean();
        boolean hasTypeParameterListBeforeFunctionName = dataStream.readBoolean();
        boolean mayHaveContract = dataStream.readBoolean();
        return new KotlinFunctionStubImpl(
                (StubElement<?>) parentStub, name, isTopLevel, fqName, isExtension, hasNoExpressionBody, hasBody,
                hasTypeParameterListBeforeFunctionName, mayHaveContract,
                mayHaveContract ? KotlinFunctionStubImpl.Companion.deserializeContract(dataStream) : null,
                KotlinStubOrigin.deserialize(dataStream)
        );
    }

    @Override
    public void indexStub(@NotNull KotlinFunctionStubImpl stub, @NotNull IndexSink sink) {
        StubIndexService.getInstance().indexFunction(stub, sink);
    }

    @NotNull
    @Override
    public String getExternalId() {
        return NAME;
    }
}
