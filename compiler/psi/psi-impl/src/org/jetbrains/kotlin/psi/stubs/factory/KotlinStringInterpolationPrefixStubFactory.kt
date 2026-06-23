/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.factory

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.*
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.psi.KtStringInterpolationPrefix
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementType
import org.jetbrains.kotlin.psi.stubs.impl.KotlinStringInterpolationPrefixStubImpl

@Suppress("UnstableApiUsage") // KT-78356: the platform stub-decoupling API is still @ApiStatus.Experimental
internal object KotlinStringInterpolationPrefixStubFactory :
    StubElementFactory<KotlinStringInterpolationPrefixStubImpl, KtStringInterpolationPrefix> {
    @OptIn(KtImplementationDetail::class)
    override fun shouldCreateStub(node: ASTNode): Boolean = KtStubElementType.shouldCreateStubDependingOnParent(node)

    override fun createStub(
        psi: KtStringInterpolationPrefix,
        parentStub: StubElement<out PsiElement>?,
    ): KotlinStringInterpolationPrefixStubImpl = KotlinStringInterpolationPrefixStubImpl(
        parent = parentStub,
        dollarSignCount = psi.interpolationPrefixElement?.textLength ?: 0,
    )

    override fun createPsi(stub: KotlinStringInterpolationPrefixStubImpl): KtStringInterpolationPrefix =
        KtStringInterpolationPrefix(stub)
}

internal object KotlinStringInterpolationPrefixStubSerializer : StubSerializer<KotlinStringInterpolationPrefixStubImpl> {
    override fun getExternalId(): String = "kotlin.STRING_INTERPOLATION_PREFIX"

    override fun serialize(stub: KotlinStringInterpolationPrefixStubImpl, dataStream: StubOutputStream) {
        dataStream.writeVarInt(stub.dollarSignCount)
    }

    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?): KotlinStringInterpolationPrefixStubImpl {
        val dollarSignCount = dataStream.readVarInt()
        return KotlinStringInterpolationPrefixStubImpl(
            parent = parentStub,
            dollarSignCount = dollarSignCount,
        )
    }

    override fun indexStub(stub: KotlinStringInterpolationPrefixStubImpl, sink: IndexSink) {
        // not indexed
    }
}
