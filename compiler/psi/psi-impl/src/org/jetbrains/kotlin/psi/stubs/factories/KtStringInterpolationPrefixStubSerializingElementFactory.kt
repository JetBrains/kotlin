/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:OptIn(KtImplementationDetail::class)

package org.jetbrains.kotlin.psi.stubs.factories

import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.psi.KtStringInterpolationPrefix
import org.jetbrains.kotlin.psi.stubs.impl.KotlinStringInterpolationPrefixStubImpl

internal object KtStringInterpolationPrefixStubSerializingElementFactory :
    KtStubSerializingElementFactory<KotlinStringInterpolationPrefixStubImpl, KtStringInterpolationPrefix>(
        type = KtNodeTypes.STRING_INTERPOLATION_PREFIX,
    ) {

    override fun createPsi(
        stub: KotlinStringInterpolationPrefixStubImpl,
    ): KtStringInterpolationPrefix = KtStringInterpolationPrefix(stub)

    override fun createStub(
        psi: KtStringInterpolationPrefix,
        parentStub: StubElement<*>?,
    ): KotlinStringInterpolationPrefixStubImpl = KotlinStringInterpolationPrefixStubImpl(
        parent = parentStub,
        dollarSignCount = psi.interpolationPrefixElement?.textLength ?: 0,
    )

    override fun serialize(stub: KotlinStringInterpolationPrefixStubImpl, dataStream: StubOutputStream) {
        dataStream.writeVarInt(stub.dollarSignCount)
    }

    override fun deserialize(
        dataStream: StubInputStream,
        parentStub: StubElement<*>?,
    ): KotlinStringInterpolationPrefixStubImpl = KotlinStringInterpolationPrefixStubImpl(
        parent = parentStub,
        dollarSignCount = dataStream.readVarInt(),
    )
}
