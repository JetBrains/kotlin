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
import org.jetbrains.kotlin.psi.KtCollectionLiteralExpression
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.psi.stubs.impl.KotlinCollectionLiteralExpressionStubImpl

internal object KtCollectionLiteralExpressionStubSerializingElementFactory :
    KtStubSerializingElementFactory<KotlinCollectionLiteralExpressionStubImpl, KtCollectionLiteralExpression>(
        type = KtNodeTypes.COLLECTION_LITERAL_EXPRESSION,
    ) {

    override fun createPsi(
        stub: KotlinCollectionLiteralExpressionStubImpl,
    ): KtCollectionLiteralExpression = KtCollectionLiteralExpression(stub)

    override fun createStub(
        psi: KtCollectionLiteralExpression,
        parentStub: StubElement<*>?,
    ): KotlinCollectionLiteralExpressionStubImpl = KotlinCollectionLiteralExpressionStubImpl(
        parent = parentStub,
        innerExpressionCount = psi.innerExpressions.size,
    )

    override fun serialize(stub: KotlinCollectionLiteralExpressionStubImpl, dataStream: StubOutputStream) {
        dataStream.writeVarInt(stub.innerExpressionCount)
    }

    override fun deserialize(
        dataStream: StubInputStream,
        parentStub: StubElement<*>?,
    ): KotlinCollectionLiteralExpressionStubImpl = KotlinCollectionLiteralExpressionStubImpl(
        parent = parentStub,
        innerExpressionCount = dataStream.readVarInt(),
    )
}
