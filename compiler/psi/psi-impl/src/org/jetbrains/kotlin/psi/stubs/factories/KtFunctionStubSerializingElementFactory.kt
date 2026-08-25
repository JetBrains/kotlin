/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(KtImplementationDetail::class)

package org.jetbrains.kotlin.psi.stubs.factories

import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.IndexSink
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.util.io.StringRef
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.safeFqNameForLazyResolve
import org.jetbrains.kotlin.psi.stubs.StubUtils.deserializeKdocText
import org.jetbrains.kotlin.psi.stubs.StubUtils.readContract
import org.jetbrains.kotlin.psi.stubs.StubUtils.serializeKdocText
import org.jetbrains.kotlin.psi.stubs.StubUtils.writeContract
import org.jetbrains.kotlin.psi.stubs.elements.StubIndexService
import org.jetbrains.kotlin.psi.stubs.impl.KotlinFunctionStubImpl
import org.jetbrains.kotlin.psi.stubs.impl.KotlinStubOrigin

internal object KtFunctionStubSerializingElementFactory :
    KtStubSerializingElementFactory<KotlinFunctionStubImpl, KtNamedFunction>(
        type = KtNodeTypes.FUN,
    ) {

    override fun createPsi(stub: KotlinFunctionStubImpl): KtNamedFunction = KtNamedFunction(stub)

    /**
     * Functions always stubbed since we want to index even local ones
     */
    override fun shouldCreateStub(node: ASTNode): Boolean = true

    override fun createStub(
        psi: KtNamedFunction,
        parentStub: StubElement<*>?,
    ): KotlinFunctionStubImpl = KotlinFunctionStubImpl(
        parent = parentStub,
        nameRef = StringRef.fromString(psi.name),
        isTopLevel = psi.parent is KtFile,
        fqName = psi.safeFqNameForLazyResolve(),
        isExtension = psi.receiverTypeReference != null,
        hasNoExpressionBody = psi.hasBlockBody(),
        hasBody = psi.hasBody(),
        hasTypeParameterListBeforeFunctionName = psi.hasTypeParameterListBeforeFunctionName(),
        mayHaveContract = psi.mayHaveContract(),
        kdocText = null,
        contract = null,
        origin = null,
    )

    override fun serialize(stub: KotlinFunctionStubImpl, dataStream: StubOutputStream) {
        dataStream.writeName(stub.name)
        dataStream.writeBoolean(stub.isTopLevel)
        dataStream.writeName(stub.fqName?.asString())
        dataStream.writeBoolean(stub.isExtension)
        dataStream.writeBoolean(stub.hasNoExpressionBody)
        dataStream.writeBoolean(stub.hasBody)
        dataStream.writeBoolean(stub.hasTypeParameterListBeforeFunctionName)

        val mayHaveContract = stub.mayHaveContract
        dataStream.writeBoolean(mayHaveContract)
        if (mayHaveContract) {
            dataStream.writeContract(stub.contract)
        }

        dataStream.serializeKdocText(stub.kdocText)

        KotlinStubOrigin.serialize(stub.origin, dataStream)
    }

    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?): KotlinFunctionStubImpl {
        val name = dataStream.readName()
        val isTopLevel = dataStream.readBoolean()
        val fqName = dataStream.readName()?.let { FqName(it.toString()) }
        val isExtension = dataStream.readBoolean()
        val hasNoExpressionBody = dataStream.readBoolean()
        val hasBody = dataStream.readBoolean()
        val hasTypeParameterListBeforeFunctionName = dataStream.readBoolean()
        val mayHaveContract = dataStream.readBoolean()
        val contract = if (mayHaveContract) dataStream.readContract() else null
        val kdocText = dataStream.deserializeKdocText()
        return KotlinFunctionStubImpl(
            parent = parentStub,
            nameRef = name,
            isTopLevel = isTopLevel,
            fqName = fqName,
            isExtension = isExtension,
            hasNoExpressionBody = hasNoExpressionBody,
            hasBody = hasBody,
            hasTypeParameterListBeforeFunctionName = hasTypeParameterListBeforeFunctionName,
            mayHaveContract = mayHaveContract,
            kdocText = kdocText,
            contract = contract,
            origin = KotlinStubOrigin.deserialize(dataStream),
        )
    }

    override fun indexStub(stub: KotlinFunctionStubImpl, sink: IndexSink) {
        StubIndexService.getInstance().indexFunction(stub, sink)
    }
}
