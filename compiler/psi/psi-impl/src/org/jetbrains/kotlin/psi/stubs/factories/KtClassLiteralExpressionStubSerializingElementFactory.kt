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
import org.jetbrains.kotlin.psi.KtClassLiteralExpression
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.psi.stubs.impl.KotlinClassLiteralExpressionStubImpl

internal object KtClassLiteralExpressionStubSerializingElementFactory :
    KtStubSerializingElementFactory<KotlinClassLiteralExpressionStubImpl, KtClassLiteralExpression>(
        type = KtNodeTypes.CLASS_LITERAL_EXPRESSION,
    ) {

    override fun createPsi(
        stub: KotlinClassLiteralExpressionStubImpl,
    ): KtClassLiteralExpression = KtClassLiteralExpression(stub)

    override fun createStub(
        psi: KtClassLiteralExpression,
        parentStub: StubElement<*>?,
    ): KotlinClassLiteralExpressionStubImpl = KotlinClassLiteralExpressionStubImpl(parentStub)

    override fun serialize(stub: KotlinClassLiteralExpressionStubImpl, dataStream: StubOutputStream) {
        // there is nothing to serialize
    }

    override fun deserialize(
        dataStream: StubInputStream,
        parentStub: StubElement<*>?,
    ): KotlinClassLiteralExpressionStubImpl = KotlinClassLiteralExpressionStubImpl(parentStub)
}
