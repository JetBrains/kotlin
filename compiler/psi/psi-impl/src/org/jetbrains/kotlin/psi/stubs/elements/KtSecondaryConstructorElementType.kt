/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.psi.stubs.elements

import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.util.io.StringRef
import org.jetbrains.annotations.NonNls
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.psi.KtSecondaryConstructor
import org.jetbrains.kotlin.psi.psiUtil.isLegacyContractPresentPsiCheck
import org.jetbrains.kotlin.psi.stubs.KotlinConstructorStub
import org.jetbrains.kotlin.psi.stubs.impl.KotlinSecondaryConstructorStubImpl
import java.io.IOException

class KtSecondaryConstructorElementType(@NonNls debugName: String) :
    KtStubElementType<KotlinSecondaryConstructorStubImpl, KtSecondaryConstructor>(
        /* debugName = */ debugName,
        /* psiClass = */ KtSecondaryConstructor::class.java,
        /* stubClass = */ KotlinConstructorStub::class.java,
    ) {

    override fun createStub(
        psi: KtSecondaryConstructor,
        parentStub: StubElement<*>,
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
        )
    }

    @Throws(IOException::class)
    override fun serialize(stub: KotlinSecondaryConstructorStubImpl, dataStream: StubOutputStream) {
        dataStream.writeName(stub.name)
        dataStream.writeBoolean(stub.hasBody())
        dataStream.writeBoolean(stub.isDelegatedCallToThis())
        dataStream.writeBoolean(stub.isExplicitDelegationCall())
        dataStream.writeBoolean(stub.mayHaveContract())
    }

    @Throws(IOException::class)
    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>): KotlinSecondaryConstructorStubImpl{
        val name = dataStream.readName()
        val hasBody = dataStream.readBoolean()
        val isDelegatedCallToThis = dataStream.readBoolean()
        val isExplicitDelegationCall = dataStream.readBoolean()
        val mayHaveContract = dataStream.readBoolean()
        return KotlinSecondaryConstructorStubImpl(
            parent = parentStub,
            containingClassName = name,
            hasBody = hasBody,
            isDelegatedCallToThis = isDelegatedCallToThis,
            isExplicitDelegationCall = isExplicitDelegationCall,
            mayHaveContract = mayHaveContract,
        )
    }
}
