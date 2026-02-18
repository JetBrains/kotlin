/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.elements;

import com.intellij.lang.ASTNode;
import com.intellij.psi.stubs.IndexSink;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.stubs.StubInputStream;
import com.intellij.psi.stubs.StubOutputStream;
import com.intellij.psi.tree.IElementType;
import com.intellij.util.io.StringRef;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.KtStubBasedElementTypes;
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

    /**
     * We want to build stubs for all non-local properties to make them indexable
     */
    @Override
    public boolean shouldCreateStub(ASTNode node) {
        ASTNode parentNode = node.getTreeParent();
        IElementType parentElementType = parentNode.getElementType();

        // Simple check for non-local properties inside classes and files
        if (parentElementType == KtStubElementTypes.CLASS_BODY || parentElementType == KtFileElementType.INSTANCE) {
            return true;
        }

        // Simple check for local and non-local properties inside blocks
        if (parentElementType == KtStubBasedElementTypes.BLOCK) {
            IElementType grandparentElementType = parentNode.getTreeParent().getElementType();
            if (grandparentElementType == KtStubElementTypes.SCRIPT) {
                return true;
            }

            if (grandparentElementType == KtStubElementTypes.FUNCTION || grandparentElementType == KtStubElementTypes.PROPERTY_ACCESSOR) {
                return false;
            }
        }

        // Fallback for psi-based check
        return !((KtProperty)node.getPsi()).isLocal();
    }

    @NotNull
    @Override
    public KotlinPropertyStubImpl createStub(@NotNull KtProperty psi, StubElement parentStub) {
        assert !psi.isLocal() :
                String.format("Should not store local property: %s, parent %s",
                              psi.getText(), psi.getParent() != null ? psi.getParent().getText() : "<no parent>");

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
                /* hasBackingField = */ null
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
