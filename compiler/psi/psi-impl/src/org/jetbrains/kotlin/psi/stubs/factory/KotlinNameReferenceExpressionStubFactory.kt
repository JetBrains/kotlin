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
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementType
import org.jetbrains.kotlin.psi.stubs.impl.KotlinNameReferenceExpressionStubImpl

@Suppress("UnstableApiUsage") // KT-78356: the platform stub-decoupling API is still @ApiStatus.Experimental
internal object KotlinNameReferenceExpressionStubFactory :
    StubElementFactory<KotlinNameReferenceExpressionStubImpl, KtNameReferenceExpression> {
    @OptIn(KtImplementationDetail::class)
    override fun shouldCreateStub(node: ASTNode): Boolean = KtStubElementType.shouldCreateStubDependingOnParent(node)

    override fun createStub(
        psi: KtNameReferenceExpression,
        parentStub: StubElement<out PsiElement>?,
    ): KotlinNameReferenceExpressionStubImpl {
        return KotlinNameReferenceExpressionStubImpl(
            parentStub,
            StringRef.fromString(psi.getReferencedName()),
            false,
        )
    }

    override fun createPsi(stub: KotlinNameReferenceExpressionStubImpl): KtNameReferenceExpression =
        KtNameReferenceExpression(stub)
}

internal object KotlinNameReferenceExpressionStubSerializer : StubSerializer<KotlinNameReferenceExpressionStubImpl> {
    override fun getExternalId(): String = "kotlin.REFERENCE_EXPRESSION"

    override fun serialize(stub: KotlinNameReferenceExpressionStubImpl, dataStream: StubOutputStream) {
        dataStream.writeName(stub.referencedName)
        dataStream.writeBoolean(stub.isClassRef)
    }

    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?): KotlinNameReferenceExpressionStubImpl {
        val referencedName = dataStream.readName()!!
        val isClassRef = dataStream.readBoolean()
        return KotlinNameReferenceExpressionStubImpl(parentStub, referencedName, isClassRef)
    }

    override fun indexStub(stub: KotlinNameReferenceExpressionStubImpl, sink: IndexSink) {
        // not indexed
    }
}
