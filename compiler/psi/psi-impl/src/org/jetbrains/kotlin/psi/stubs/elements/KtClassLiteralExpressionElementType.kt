/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.elements

import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import org.jetbrains.annotations.NonNls
import org.jetbrains.kotlin.psi.KtClassLiteralExpression
import org.jetbrains.kotlin.psi.stubs.KotlinClassLiteralExpressionStub
import org.jetbrains.kotlin.psi.stubs.impl.KotlinClassLiteralExpressionStubImpl

class KtClassLiteralExpressionElementType(@NonNls debugName: String) :
    KtStubElementType<KotlinClassLiteralExpressionStubImpl, KtClassLiteralExpression>(
        debugName,
        KtClassLiteralExpression::class.java,
        KotlinClassLiteralExpressionStub::class.java,
    ) {

    override fun createStub(psi: KtClassLiteralExpression, parentStub: StubElement<*>?): KotlinClassLiteralExpressionStubImpl {
        return KotlinClassLiteralExpressionStubImpl(parentStub)
    }

    override fun serialize(stub: KotlinClassLiteralExpressionStubImpl, dataStream: StubOutputStream) {
    }

    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?): KotlinClassLiteralExpressionStubImpl {
        return KotlinClassLiteralExpressionStubImpl(parentStub)
    }
}