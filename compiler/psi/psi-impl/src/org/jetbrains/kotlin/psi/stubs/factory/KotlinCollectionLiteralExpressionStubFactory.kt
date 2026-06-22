/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.factory

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.*
import org.jetbrains.kotlin.psi.KtCollectionLiteralExpression
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementType
import org.jetbrains.kotlin.psi.stubs.impl.KotlinCollectionLiteralExpressionStubImpl

@Suppress("UnstableApiUsage") // KT-78356: the platform stub-decoupling API is still @ApiStatus.Experimental
internal object KotlinCollectionLiteralExpressionStubFactory :
    StubElementFactory<KotlinCollectionLiteralExpressionStubImpl, KtCollectionLiteralExpression> {
    override fun shouldCreateStub(node: ASTNode): Boolean = KtStubElementType.shouldCreateStubDependingOnParent(node)

    override fun createStub(
        psi: KtCollectionLiteralExpression,
        parentStub: StubElement<out PsiElement>?,
    ): KotlinCollectionLiteralExpressionStubImpl {
        return KotlinCollectionLiteralExpressionStubImpl(
            parent = parentStub,
            innerExpressionCount = psi.innerExpressions.size,
        )
    }

    override fun createPsi(stub: KotlinCollectionLiteralExpressionStubImpl): KtCollectionLiteralExpression =
        KtCollectionLiteralExpression(stub)
}

internal object KotlinCollectionLiteralExpressionStubSerializer : StubSerializer<KotlinCollectionLiteralExpressionStubImpl> {
    override fun getExternalId(): String = "kotlin.COLLECTION_LITERAL_EXPRESSION"

    override fun serialize(stub: KotlinCollectionLiteralExpressionStubImpl, dataStream: StubOutputStream) {
        dataStream.writeVarInt(stub.innerExpressionCount)
    }

    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?): KotlinCollectionLiteralExpressionStubImpl {
        val innerExpressionCount = dataStream.readVarInt()
        return KotlinCollectionLiteralExpressionStubImpl(
            parent = parentStub,
            innerExpressionCount = innerExpressionCount,
        )
    }

    override fun indexStub(stub: KotlinCollectionLiteralExpressionStubImpl, sink: IndexSink) {
        // not indexed
    }
}
