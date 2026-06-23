/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("UnstableApiUsage") // KT-78356: the platform stub-decoupling API is still @ApiStatus.Experimental

package org.jetbrains.kotlin.psi.stubs.elements

import com.intellij.psi.stubs.StubElementFactory
import com.intellij.psi.stubs.StubSerializer
import org.jetbrains.annotations.NonNls
import org.jetbrains.kotlin.psi.KtClassLiteralExpression
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.psi.stubs.KotlinClassLiteralExpressionStub
import org.jetbrains.kotlin.psi.stubs.factory.KotlinClassLiteralExpressionStubFactory
import org.jetbrains.kotlin.psi.stubs.factory.KotlinClassLiteralExpressionStubSerializer
import org.jetbrains.kotlin.psi.stubs.impl.KotlinClassLiteralExpressionStubImpl

class KtClassLiteralExpressionElementType(@NonNls debugName: String) :
    KtStubElementType<KotlinClassLiteralExpressionStubImpl, KtClassLiteralExpression>(
        debugName,
        KtClassLiteralExpression::class.java,
        KotlinClassLiteralExpressionStub::class.java,
    ) {
    @KtImplementationDetail
    override fun getStubFactory(): StubElementFactory<KotlinClassLiteralExpressionStubImpl, KtClassLiteralExpression> =
        KotlinClassLiteralExpressionStubFactory

    @KtImplementationDetail
    override fun getStubSerializer(): StubSerializer<KotlinClassLiteralExpressionStubImpl> =
        KotlinClassLiteralExpressionStubSerializer
}
