/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.factory

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.*
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.psi.KtPropertyAccessor
import org.jetbrains.kotlin.psi.psiUtil.isLegacyContractPresentPsiCheck
import org.jetbrains.kotlin.psi.stubs.StubUtils.readContract
import org.jetbrains.kotlin.psi.stubs.StubUtils.writeContract
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementType
import org.jetbrains.kotlin.psi.stubs.impl.KotlinPropertyAccessorStubImpl

@Suppress("UnstableApiUsage") // KT-78356: the platform stub-decoupling API is still @ApiStatus.Experimental
internal object KotlinPropertyAccessorStubFactory : StubElementFactory<KotlinPropertyAccessorStubImpl, KtPropertyAccessor> {
    override fun shouldCreateStub(node: ASTNode): Boolean = KtStubElementType.shouldCreateStubDependingOnParent(node)

    @OptIn(KtImplementationDetail::class)
    override fun createStub(psi: KtPropertyAccessor, parentStub: StubElement<out PsiElement>?): KotlinPropertyAccessorStubImpl {
        return KotlinPropertyAccessorStubImpl(
            parent = parentStub,
            isGetter = psi.isGetter,
            hasBody = psi.hasBody(),
            hasNoExpressionBody = psi.hasBlockBody(),
            mayHaveContract = psi.isLegacyContractPresentPsiCheck(),
            contract = null,
        )
    }

    override fun createPsi(stub: KotlinPropertyAccessorStubImpl): KtPropertyAccessor = KtPropertyAccessor(stub)
}

internal object KotlinPropertyAccessorStubSerializer : StubSerializer<KotlinPropertyAccessorStubImpl> {
    override fun getExternalId(): String = "kotlin.PROPERTY_ACCESSOR"

    override fun serialize(stub: KotlinPropertyAccessorStubImpl, dataStream: StubOutputStream) {
        dataStream.writeBoolean(stub.isGetter)
        dataStream.writeBoolean(stub.hasBody)
        dataStream.writeBoolean(stub.hasNoExpressionBody)
        val mayHaveContract = stub.mayHaveContract
        dataStream.writeBoolean(mayHaveContract)
        if (mayHaveContract) {
            dataStream.writeContract(stub.contract)
        }
    }

    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?): KotlinPropertyAccessorStubImpl {
        val isGetter = dataStream.readBoolean()
        val hasBody = dataStream.readBoolean()
        val hasNoExpressionBody = dataStream.readBoolean()
        val mayHaveContract = dataStream.readBoolean()
        val contract = if (mayHaveContract) dataStream.readContract() else null

        return KotlinPropertyAccessorStubImpl(
            parent = parentStub,
            isGetter = isGetter,
            hasBody = hasBody,
            hasNoExpressionBody = hasNoExpressionBody,
            mayHaveContract = mayHaveContract,
            contract = contract,
        )
    }

    override fun indexStub(stub: KotlinPropertyAccessorStubImpl, sink: IndexSink) {
        // not indexed
    }
}
