/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.psi.stubs.elements

import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import org.jetbrains.annotations.NonNls
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.psi.KtPropertyAccessor
import org.jetbrains.kotlin.psi.psiUtil.isLegacyContractPresentPsiCheck
import org.jetbrains.kotlin.psi.stubs.KotlinPropertyAccessorStub
import org.jetbrains.kotlin.psi.stubs.impl.KotlinPropertyAccessorStubImpl
import java.io.IOException

object KtPropertyAccessorElementType : KtStubElementType<KotlinPropertyAccessorStubImpl, KtPropertyAccessor>(
    "PROPERTY_ACCESSOR",
    KtPropertyAccessor::class.java,
    KotlinPropertyAccessorStub::class.java,
) {
    @OptIn(KtImplementationDetail::class)
    override fun createStub(psi: KtPropertyAccessor, parentStub: StubElement<*>?): KotlinPropertyAccessorStubImpl {
        return KotlinPropertyAccessorStubImpl(
            parent = parentStub,
            isGetter = psi.isGetter,
            hasBody = psi.hasBody(),
            hasNoExpressionBody = psi.hasBlockBody(),
            mayHaveContract = psi.isLegacyContractPresentPsiCheck(),
        )
    }

    override fun serialize(stub: KotlinPropertyAccessorStubImpl, dataStream: StubOutputStream) {
        dataStream.writeBoolean(stub.isGetter)
        dataStream.writeBoolean(stub.hasBody)
        dataStream.writeBoolean(stub.hasNoExpressionBody)
        dataStream.writeBoolean(stub.mayHaveContract)
    }

    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?): KotlinPropertyAccessorStubImpl {
        val isGetter = dataStream.readBoolean()
        val hasBody = dataStream.readBoolean()
        val hasNoExpressionBody = dataStream.readBoolean()
        val mayHaveContract = dataStream.readBoolean()
        return KotlinPropertyAccessorStubImpl(
            parent = parentStub,
            isGetter = isGetter,
            hasBody = hasBody,
            hasNoExpressionBody = hasNoExpressionBody,
            mayHaveContract = mayHaveContract,
        )
    }
}
