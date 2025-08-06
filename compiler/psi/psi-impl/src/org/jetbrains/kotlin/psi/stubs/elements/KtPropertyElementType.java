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
import org.jetbrains.kotlin.constant.ConstantValue;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.psi.KtProperty;
import org.jetbrains.kotlin.psi.psiUtil.KtPsiUtilKt;
import org.jetbrains.kotlin.psi.stubs.KotlinPropertyStub;
import org.jetbrains.kotlin.psi.stubs.StubUtils;
import org.jetbrains.kotlin.psi.stubs.impl.KotlinConstantValueKt;
import org.jetbrains.kotlin.psi.stubs.impl.KotlinPropertyStubImpl;
import org.jetbrains.kotlin.psi.stubs.impl.KotlinStubOrigin;

import java.io.IOException;

public class KtPropertyElementType extends KtStubElementType<KotlinPropertyStubImpl, KtProperty> {
    public KtPropertyElementType(@NotNull @NonNls String debugName) {
        super(debugName, KtProperty.class, KotlinPropertyStub.class);
    }

    @NotNull
    @Override
    public KotlinPropertyStubImpl createStub(@NotNull KtProperty psi, StubElement parentStub) {
        assert !psi.isLocal() :
                String.format("Should not store local property: %s, parent %s",
                              psi.getText(), psi.getParent() != null ? psi.getParent().getText() : "<no parent>");

        Boolean hasBackingField = StubUtils.searchForHasBackingFieldComment$psi_impl(psi);
        return new KotlinPropertyStubImpl(
                (StubElement<?>) parentStub,
                StringRef.fromString(psi.getName()),
                psi.isVar(),
                psi.isTopLevel(),
                psi.hasDelegate(),
                psi.hasDelegateExpression(),
                psi.hasInitializer(),
                psi.getReceiverTypeReference() != null,
                psi.getTypeReference() != null,
                KtPsiUtilKt.safeFqNameForLazyResolve(psi),
                /* constantInitializer = */ null,
                /* origin = */ null,
                /* hasBackingField = */hasBackingField
        );
    }

    @Override
    public void serialize(@NotNull KotlinPropertyStubImpl stub, @NotNull StubOutputStream dataStream) throws IOException {
        dataStream.writeName(stub.getName());
        dataStream.writeBoolean(stub.isVar());
        dataStream.writeBoolean(stub.isTopLevel());
        dataStream.writeBoolean(stub.getHasDelegate());
        dataStream.writeBoolean(stub.getHasDelegateExpression());
        dataStream.writeBoolean(stub.getHasInitializer());
        dataStream.writeBoolean(stub.isExtension());
        dataStream.writeBoolean(stub.getHasReturnTypeRef());

        FqName fqName = stub.getFqName();
        dataStream.writeName(fqName != null ? fqName.asString() : null);

        ConstantValue<?> constantInitializer = stub.getConstantInitializer();
        KotlinConstantValueKt.serializeConstantValue(constantInitializer, dataStream);

        KotlinStubOrigin.serialize(stub.getOrigin(), dataStream);

        StubUtils.writeNullableBoolean$psi_impl(dataStream, stub.getHasBackingField());
    }

    @NotNull
    @Override
    public KotlinPropertyStubImpl deserialize(@NotNull StubInputStream dataStream, StubElement parentStub) throws IOException {
        StringRef name = dataStream.readName();
        boolean isVar = dataStream.readBoolean();
        boolean isTopLevel = dataStream.readBoolean();
        boolean hasDelegate = dataStream.readBoolean();
        boolean hasDelegateExpression = dataStream.readBoolean();
        boolean hasInitializer = dataStream.readBoolean();
        boolean hasReceiverTypeRef = dataStream.readBoolean();
        boolean hasReturnTypeRef = dataStream.readBoolean();

        StringRef fqNameAsString = dataStream.readName();
        FqName fqName = fqNameAsString != null ? new FqName(fqNameAsString.toString()) : null;

        ConstantValue<?> constantInitializer = KotlinConstantValueKt.deserializeConstantValue(dataStream);
        KotlinStubOrigin stubOrigin = KotlinStubOrigin.deserialize(dataStream);
        Boolean hasBackingFiled = StubUtils.readNullableBoolean$psi_impl(dataStream);
        return new KotlinPropertyStubImpl(
                (StubElement<?>) parentStub,
                name,
                isVar,
                isTopLevel,
                hasDelegate,
                hasDelegateExpression,
                hasInitializer,
                hasReceiverTypeRef,
                hasReturnTypeRef,
                fqName,
                constantInitializer,
                stubOrigin,
                hasBackingFiled
        );
    }

    @Override
    public void indexStub(@NotNull KotlinPropertyStubImpl stub, @NotNull IndexSink sink) {
        StubIndexService.getInstance().indexProperty(stub, sink);
    }
}
