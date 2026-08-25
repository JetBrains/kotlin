/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(KtImplementationDetail::class)

package org.jetbrains.kotlin.psi.stubs.factories

import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.util.io.StringRef
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.psi.KtSecondaryConstructor
import org.jetbrains.kotlin.psi.psiUtil.isLegacyContractPresentPsiCheck
import org.jetbrains.kotlin.psi.stubs.StubUtils.deserializeKdocText
import org.jetbrains.kotlin.psi.stubs.StubUtils.serializeKdocText
import org.jetbrains.kotlin.psi.stubs.impl.KotlinSecondaryConstructorStubImpl

internal object KtSecondaryConstructorStubSerializingElementFactory :
    KtStubSerializingElementFactory<KotlinSecondaryConstructorStubImpl, KtSecondaryConstructor>(
        type = KtNodeTypes.SECONDARY_CONSTRUCTOR,
    ) {

    override fun createPsi(
        stub: KotlinSecondaryConstructorStubImpl,
    ): KtSecondaryConstructor = KtSecondaryConstructor(stub)

    override fun createStub(
        psi: KtSecondaryConstructor,
        parentStub: StubElement<*>?,
    ): KotlinSecondaryConstructorStubImpl {
        val hasBody = psi.hasBody()
        val delegationCall = psi.getDelegationCallOrNull()
        val isDelegatedCallToThis = delegationCall?.isCallToThis ?: true
        val isExplicitDelegationCall = delegationCall?.isImplicit == false
        val mayHaveContract = psi.isLegacyContractPresentPsiCheck()
        return KotlinSecondaryConstructorStubImpl(
            parent = parentStub,
            containingClassName = StringRef.fromString(psi.name),
            hasBody = hasBody,
            isDelegatedCallToThis = isDelegatedCallToThis,
            isExplicitDelegationCall = isExplicitDelegationCall,
            mayHaveContract = mayHaveContract,
            kdocText = null,
        )
    }

    override fun serialize(stub: KotlinSecondaryConstructorStubImpl, dataStream: StubOutputStream) {
        dataStream.writeName(stub.name)
        dataStream.writeBoolean(stub.hasBody)
        dataStream.writeBoolean(stub.isDelegatedCallToThis)
        dataStream.writeBoolean(stub.isExplicitDelegationCall)
        dataStream.writeBoolean(stub.mayHaveContract)
        dataStream.serializeKdocText(stub.kdocText)
    }

    override fun deserialize(
        dataStream: StubInputStream,
        parentStub: StubElement<*>?,
    ): KotlinSecondaryConstructorStubImpl {
        val name = dataStream.readName()
        val hasBody = dataStream.readBoolean()
        val isDelegatedCallToThis = dataStream.readBoolean()
        val isExplicitDelegationCall = dataStream.readBoolean()
        val mayHaveContract = dataStream.readBoolean()
        val kdocText = dataStream.deserializeKdocText()
        return KotlinSecondaryConstructorStubImpl(
            parent = parentStub,
            containingClassName = name,
            hasBody = hasBody,
            isDelegatedCallToThis = isDelegatedCallToThis,
            isExplicitDelegationCall = isExplicitDelegationCall,
            mayHaveContract = mayHaveContract,
            kdocText = kdocText,
        )
    }
}
