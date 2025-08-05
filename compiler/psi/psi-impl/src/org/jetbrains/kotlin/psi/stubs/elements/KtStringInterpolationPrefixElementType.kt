/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.elements

import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import org.jetbrains.kotlin.psi.KtStringInterpolationPrefix
import org.jetbrains.kotlin.psi.stubs.KotlinStringInterpolationPrefixStub
import org.jetbrains.kotlin.psi.stubs.impl.KotlinStringInterpolationPrefixStubImpl

class KtStringInterpolationPrefixElementType(debugName: String) :
    KtStubElementType<KotlinStringInterpolationPrefixStubImpl, KtStringInterpolationPrefix>(
        debugName,
        KtStringInterpolationPrefix::class.java,
        KotlinStringInterpolationPrefixStub::class.java,
    ) {

    override fun createStub(
        psi: KtStringInterpolationPrefix,
        parentStub: StubElement<out PsiElement?>?,
    ): KotlinStringInterpolationPrefixStubImpl = KotlinStringInterpolationPrefixStubImpl(
        parent = parentStub,
        dollarSignCount = psi.interpolationPrefixElement?.textLength ?: 0,
    )

    override fun serialize(
        stub: KotlinStringInterpolationPrefixStubImpl,
        dataStream: StubOutputStream,
    ) {
        dataStream.writeVarInt(stub.dollarSignCount)
    }

    override fun deserialize(
        dataStream: StubInputStream,
        parentStub: StubElement<*>?,
    ): KotlinStringInterpolationPrefixStubImpl {
        val dollarSignCount = dataStream.readVarInt()
        return KotlinStringInterpolationPrefixStubImpl(
            parent = parentStub,
            dollarSignCount = dollarSignCount,
        )
    }
}
