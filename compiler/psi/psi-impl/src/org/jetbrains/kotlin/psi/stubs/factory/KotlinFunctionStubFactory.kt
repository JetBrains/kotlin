/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.factory

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.*
import com.intellij.util.io.StringRef
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.safeFqNameForLazyResolve
import org.jetbrains.kotlin.psi.stubs.StubUtils
import org.jetbrains.kotlin.psi.stubs.StubUtils.deserializeKdocText
import org.jetbrains.kotlin.psi.stubs.StubUtils.serializeKdocText
import org.jetbrains.kotlin.psi.stubs.elements.StubIndexService
import org.jetbrains.kotlin.psi.stubs.impl.KotlinFunctionStubImpl
import org.jetbrains.kotlin.psi.stubs.impl.KotlinStubOrigin

@Suppress("UnstableApiUsage") // KT-78356: the platform stub-decoupling API is still @ApiStatus.Experimental
internal object KotlinFunctionStubFactory : StubElementFactory<KotlinFunctionStubImpl, KtNamedFunction> {
    /**
     * Functions are always stubbed since we want to index even local ones.
     */
    override fun shouldCreateStub(node: ASTNode): Boolean = true

    override fun createStub(psi: KtNamedFunction, parentStub: StubElement<out PsiElement>?): KotlinFunctionStubImpl {
        val isTopLevel = psi.parent is KtFile
        val isExtension = psi.receiverTypeReference != null
        val fqName = psi.safeFqNameForLazyResolve()
        val hasNoExpressionBody = psi.hasBlockBody()
        val hasBody = psi.hasBody()
        return KotlinFunctionStubImpl(
            parentStub,
            StringRef.fromString(psi.name),
            isTopLevel,
            fqName,
            isExtension,
            hasNoExpressionBody,
            hasBody,
            psi.hasTypeParameterListBeforeFunctionName(),
            psi.mayHaveContract(),
            /* kdocText = */ null,
            /* contract = */ null,
            /* origin = */ null,
        )
    }

    override fun createPsi(stub: KotlinFunctionStubImpl): KtNamedFunction = KtNamedFunction(stub)
}

internal object KotlinFunctionStubSerializer : StubSerializer<KotlinFunctionStubImpl> {
    override fun getExternalId(): String = "kotlin.FUNCTION"

    override fun serialize(stub: KotlinFunctionStubImpl, dataStream: StubOutputStream) {
        dataStream.writeName(stub.name)
        dataStream.writeBoolean(stub.isTopLevel)
        dataStream.writeName(stub.fqName?.asString())
        dataStream.writeBoolean(stub.isExtension)
        dataStream.writeBoolean(stub.hasNoExpressionBody)
        dataStream.writeBoolean(stub.hasBody)
        dataStream.writeBoolean(stub.hasTypeParameterListBeforeFunctionName)
        val haveContract = stub.mayHaveContract
        dataStream.writeBoolean(haveContract)
        if (haveContract) {
            with(StubUtils) { dataStream.writeContract(stub.contract) }
        }
        dataStream.serializeKdocText(stub.kdocText)
        KotlinStubOrigin.serialize(stub.origin, dataStream)
    }

    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?): KotlinFunctionStubImpl {
        val name = dataStream.readName()
        val isTopLevel = dataStream.readBoolean()
        val fqNameAsString = dataStream.readName()
        val fqName = fqNameAsString?.let { FqName(it.toString()) }
        val isExtension = dataStream.readBoolean()
        val hasNoExpressionBody = dataStream.readBoolean()
        val hasBody = dataStream.readBoolean()
        val hasTypeParameterListBeforeFunctionName = dataStream.readBoolean()
        val mayHaveContract = dataStream.readBoolean()
        val contract = if (mayHaveContract) with(StubUtils) { dataStream.readContract() } else null
        val kdocText = dataStream.deserializeKdocText()
        return KotlinFunctionStubImpl(
            parentStub,
            name,
            isTopLevel,
            fqName,
            isExtension,
            hasNoExpressionBody,
            hasBody,
            hasTypeParameterListBeforeFunctionName,
            mayHaveContract,
            kdocText,
            contract,
            KotlinStubOrigin.deserialize(dataStream),
        )
    }

    override fun indexStub(stub: KotlinFunctionStubImpl, sink: IndexSink) {
        StubIndexService.getInstance().indexFunction(stub, sink)
    }
}
