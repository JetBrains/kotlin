/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.psi.stubs.elements

import com.intellij.psi.stubs.IndexSink
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.util.io.StringRef
import org.jetbrains.annotations.NonNls
import org.jetbrains.kotlin.psi.KtConstructor
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.psi.psiUtil.isContractPresentPsiCheck
import org.jetbrains.kotlin.psi.stubs.KotlinConstructorStub
import java.io.IOException

abstract class KtConstructorElementType<T : KtConstructor<T>>(
    @NonNls debugName: String,
    tClass: Class<T>,
    stubClass: Class<KotlinConstructorStub<*>>
) : KtStubElementType<KotlinConstructorStub<T>, T>(debugName, tClass, stubClass) {
    protected abstract fun newStub(
        parentStub: StubElement<*>,
        nameRef: StringRef?,
        hasBody: Boolean,
        isDelegatedCallToThis: Boolean,
        isExplicitDelegationCall: Boolean,
        mayHaveContract: Boolean,
    ): KotlinConstructorStub<T>

    protected abstract fun isDelegatedCallToThis(constructor: T): Boolean

    protected abstract fun isExplicitDelegationCall(constructor: T): Boolean

    override fun createStub(psi: T, parentStub: StubElement<*>): KotlinConstructorStub<T> {
        val hasBody = psi.hasBody()
        val isDelegatedCallToThis = isDelegatedCallToThis(psi)
        val isExplicitDelegationCall = isExplicitDelegationCall(psi)

        @OptIn(KtImplementationDetail::class)
        val mayHaveContract = psi.isContractPresentPsiCheck()
        return newStub(
            parentStub = parentStub,
            nameRef = StringRef.fromString(psi.name),
            hasBody = hasBody,
            isDelegatedCallToThis = isDelegatedCallToThis,
            isExplicitDelegationCall = isExplicitDelegationCall,
            mayHaveContract = mayHaveContract,
        )
    }

    @Throws(IOException::class)
    override fun serialize(stub: KotlinConstructorStub<T>, dataStream: StubOutputStream) {
        dataStream.writeName(stub.name)
        dataStream.writeBoolean(stub.hasBody())
        dataStream.writeBoolean(stub.isDelegatedCallToThis())
        dataStream.writeBoolean(stub.isExplicitDelegationCall())
        dataStream.writeBoolean(stub.mayHaveContract())
    }

    @Throws(IOException::class)
    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>): KotlinConstructorStub<T> {
        val name = dataStream.readName()
        val hasBody = dataStream.readBoolean()
        val isDelegatedCallToThis = dataStream.readBoolean()
        val isExplicitDelegationCall = dataStream.readBoolean()
        val mayHaveContract = dataStream.readBoolean()
        return newStub(
            parentStub = parentStub,
            nameRef = name,
            hasBody = hasBody,
            isDelegatedCallToThis = isDelegatedCallToThis,
            isExplicitDelegationCall = isExplicitDelegationCall,
            mayHaveContract = mayHaveContract,
        )
    }

    override fun indexStub(stub: KotlinConstructorStub<T>, sink: IndexSink) {
    }
}