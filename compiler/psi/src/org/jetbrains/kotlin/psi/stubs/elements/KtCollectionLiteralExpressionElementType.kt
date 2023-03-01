/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.elements

import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import org.jetbrains.annotations.NonNls
import org.jetbrains.kotlin.psi.KtClassLiteralExpression
import org.jetbrains.kotlin.psi.KtCollectionLiteralExpression
import org.jetbrains.kotlin.psi.stubs.KotlinClassLiteralExpressionStub
import org.jetbrains.kotlin.psi.stubs.KotlinCollectionLiteralExpressionStub
import org.jetbrains.kotlin.psi.stubs.impl.KotlinClassLiteralExpressionStubImpl
import org.jetbrains.kotlin.psi.stubs.impl.KotlinCollectionLiteralExpressionStubImpl

class KtCollectionLiteralExpressionElementType(@NonNls debugName: String) :
    KtStubElementType<KotlinCollectionLiteralExpressionStub, KtCollectionLiteralExpression>(
        debugName,
        KtCollectionLiteralExpression::class.java,
        KotlinCollectionLiteralExpressionStub::class.java
    ) {
    override fun serialize(stub: KotlinCollectionLiteralExpressionStub, dataStream: StubOutputStream) {}

    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?): KotlinCollectionLiteralExpressionStub {
        return KotlinCollectionLiteralExpressionStubImpl(parentStub)
    }

    override fun createStub(psi: KtCollectionLiteralExpression, parentStub: StubElement<*>?): KotlinCollectionLiteralExpressionStub {
        return KotlinCollectionLiteralExpressionStubImpl(parentStub)
    }
}