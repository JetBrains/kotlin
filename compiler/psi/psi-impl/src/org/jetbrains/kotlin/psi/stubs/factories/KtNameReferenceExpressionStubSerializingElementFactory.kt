/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:OptIn(KtImplementationDetail::class)

package org.jetbrains.kotlin.psi.stubs.factories

import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.util.io.StringRef
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.stubs.impl.KotlinNameReferenceExpressionStubImpl

internal object KtNameReferenceExpressionStubSerializingElementFactory :
    KtStubSerializingElementFactory<KotlinNameReferenceExpressionStubImpl, KtNameReferenceExpression>(
        type = KtNodeTypes.REFERENCE_EXPRESSION,
    ) {

    override fun createPsi(
        stub: KotlinNameReferenceExpressionStubImpl,
    ): KtNameReferenceExpression = KtNameReferenceExpression(stub)

    override fun createStub(
        psi: KtNameReferenceExpression,
        parentStub: StubElement<*>?,
    ): KotlinNameReferenceExpressionStubImpl = KotlinNameReferenceExpressionStubImpl(
        parentStub,
        StringRef.fromString(psi.getReferencedName())!!,
        /* myClassRef = */ false,
    )

    override fun serialize(stub: KotlinNameReferenceExpressionStubImpl, dataStream: StubOutputStream) {
        dataStream.writeName(stub.referencedName)
        dataStream.writeBoolean(stub.isClassRef)
    }

    override fun deserialize(
        dataStream: StubInputStream,
        parentStub: StubElement<*>?,
    ): KotlinNameReferenceExpressionStubImpl = KotlinNameReferenceExpressionStubImpl(
        parentStub,
        dataStream.readName()!!,
        /* myClassRef = */ dataStream.readBoolean(),
    )
}
