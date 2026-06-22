/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.factory

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.*
import org.jetbrains.kotlin.psi.KtClassLiteralExpression
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementType
import org.jetbrains.kotlin.psi.stubs.impl.KotlinClassLiteralExpressionStubImpl

@Suppress("UnstableApiUsage") // KT-78356: the platform stub-decoupling API is still @ApiStatus.Experimental
internal object KotlinClassLiteralExpressionStubFactory :
    StubElementFactory<KotlinClassLiteralExpressionStubImpl, KtClassLiteralExpression> {
    override fun shouldCreateStub(node: ASTNode): Boolean = KtStubElementType.shouldCreateStubDependingOnParent(node)

    override fun createStub(
        psi: KtClassLiteralExpression,
        parentStub: StubElement<out PsiElement>?,
    ): KotlinClassLiteralExpressionStubImpl {
        return KotlinClassLiteralExpressionStubImpl(parentStub)
    }

    override fun createPsi(stub: KotlinClassLiteralExpressionStubImpl): KtClassLiteralExpression =
        KtClassLiteralExpression(stub)
}

internal object KotlinClassLiteralExpressionStubSerializer : StubSerializer<KotlinClassLiteralExpressionStubImpl> {
    override fun getExternalId(): String = "kotlin.CLASS_LITERAL_EXPRESSION"

    override fun serialize(stub: KotlinClassLiteralExpressionStubImpl, dataStream: StubOutputStream) {
    }

    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?): KotlinClassLiteralExpressionStubImpl {
        return KotlinClassLiteralExpressionStubImpl(parentStub)
    }

    override fun indexStub(stub: KotlinClassLiteralExpressionStubImpl, sink: IndexSink) {
        // not indexed
    }
}
