/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("UnstableApiUsage") // KT-78356: the platform stub-decoupling API is still @ApiStatus.Experimental

package org.jetbrains.kotlin.psi.stubs.elements

import com.intellij.psi.stubs.StubElementFactory
import com.intellij.psi.stubs.StubSerializer
import org.jetbrains.annotations.NonNls
import org.jetbrains.kotlin.psi.KtCollectionLiteralExpression
import org.jetbrains.kotlin.psi.stubs.KotlinCollectionLiteralExpressionStub
import org.jetbrains.kotlin.psi.stubs.factory.KotlinCollectionLiteralExpressionStubFactory
import org.jetbrains.kotlin.psi.stubs.factory.KotlinCollectionLiteralExpressionStubSerializer
import org.jetbrains.kotlin.psi.stubs.impl.KotlinCollectionLiteralExpressionStubImpl

class KtCollectionLiteralExpressionElementType(@NonNls debugName: String) :
    KtStubElementType<KotlinCollectionLiteralExpressionStubImpl, KtCollectionLiteralExpression>(
        debugName,
        KtCollectionLiteralExpression::class.java,
        KotlinCollectionLiteralExpressionStub::class.java,
    ) {
    override fun getStubFactory(): StubElementFactory<KotlinCollectionLiteralExpressionStubImpl, KtCollectionLiteralExpression> =
        KotlinCollectionLiteralExpressionStubFactory

    override fun getStubSerializer(): StubSerializer<KotlinCollectionLiteralExpressionStubImpl> =
        KotlinCollectionLiteralExpressionStubSerializer
}
