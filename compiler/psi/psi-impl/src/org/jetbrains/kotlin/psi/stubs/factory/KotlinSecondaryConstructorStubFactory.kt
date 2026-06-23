/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.factory

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.*
import com.intellij.util.io.StringRef
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.psi.KtSecondaryConstructor
import org.jetbrains.kotlin.psi.psiUtil.isLegacyContractPresentPsiCheck
import org.jetbrains.kotlin.psi.stubs.StubUtils.deserializeKdocText
import org.jetbrains.kotlin.psi.stubs.StubUtils.serializeKdocText
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementType
import org.jetbrains.kotlin.psi.stubs.impl.KotlinSecondaryConstructorStubImpl

@Suppress("UnstableApiUsage") // KT-78356: the platform stub-decoupling API is still @ApiStatus.Experimental
internal object KotlinSecondaryConstructorStubFactory : StubElementFactory<KotlinSecondaryConstructorStubImpl, KtSecondaryConstructor> {
    @OptIn(KtImplementationDetail::class)
    override fun shouldCreateStub(node: ASTNode): Boolean = KtStubElementType.shouldCreateStubDependingOnParent(node)

    override fun createStub(
        psi: KtSecondaryConstructor,
        parentStub: StubElement<out PsiElement>?,
    ): KotlinSecondaryConstructorStubImpl {
        val hasBody = psi.hasBody()
        val isDelegatedCallToThis = psi.getDelegationCallOrNull()?.isCallToThis ?: true
        val isExplicitDelegationCall = psi.getDelegationCallOrNull()?.isImplicit == false

        @OptIn(KtImplementationDetail::class)
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

    override fun createPsi(stub: KotlinSecondaryConstructorStubImpl): KtSecondaryConstructor =
        KtSecondaryConstructor(stub)
}

internal object KotlinSecondaryConstructorStubSerializer : StubSerializer<KotlinSecondaryConstructorStubImpl> {
    override fun getExternalId(): String = "kotlin.SECONDARY_CONSTRUCTOR"

    override fun serialize(stub: KotlinSecondaryConstructorStubImpl, dataStream: StubOutputStream) {
        dataStream.writeName(stub.name)
        dataStream.writeBoolean(stub.hasBody)
        dataStream.writeBoolean(stub.isDelegatedCallToThis)
        dataStream.writeBoolean(stub.isExplicitDelegationCall)
        dataStream.writeBoolean(stub.mayHaveContract)
        dataStream.serializeKdocText(stub.kdocText)
    }

    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?): KotlinSecondaryConstructorStubImpl {
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

    override fun indexStub(stub: KotlinSecondaryConstructorStubImpl, sink: IndexSink) {
        // not indexed
    }
}
