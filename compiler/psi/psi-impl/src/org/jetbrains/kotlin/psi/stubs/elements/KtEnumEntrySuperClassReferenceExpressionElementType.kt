/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("UnstableApiUsage") // KT-78356: the platform stub-decoupling API is still @ApiStatus.Experimental

package org.jetbrains.kotlin.psi.stubs.elements

import com.intellij.psi.stubs.StubElementFactory
import com.intellij.psi.stubs.StubSerializer
import org.jetbrains.annotations.NonNls
import org.jetbrains.kotlin.psi.KtEnumEntrySuperclassReferenceExpression
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.psi.stubs.KotlinEnumEntrySuperclassReferenceExpressionStub
import org.jetbrains.kotlin.psi.stubs.factory.KotlinEnumEntrySuperclassReferenceExpressionStubFactory
import org.jetbrains.kotlin.psi.stubs.factory.KotlinEnumEntrySuperclassReferenceExpressionStubSerializer
import org.jetbrains.kotlin.psi.stubs.impl.KotlinEnumEntrySuperclassReferenceExpressionStubImpl

class KtEnumEntrySuperClassReferenceExpressionElementType(@NonNls debugName: String) :
    KtStubElementType<KotlinEnumEntrySuperclassReferenceExpressionStubImpl, KtEnumEntrySuperclassReferenceExpression>(
        debugName,
        KtEnumEntrySuperclassReferenceExpression::class.java,
        KotlinEnumEntrySuperclassReferenceExpressionStub::class.java,
    ) {
    @KtImplementationDetail
    override fun getStubFactory(): StubElementFactory<KotlinEnumEntrySuperclassReferenceExpressionStubImpl, KtEnumEntrySuperclassReferenceExpression> =
        KotlinEnumEntrySuperclassReferenceExpressionStubFactory

    @KtImplementationDetail
    override fun getStubSerializer(): StubSerializer<KotlinEnumEntrySuperclassReferenceExpressionStubImpl> =
        KotlinEnumEntrySuperclassReferenceExpressionStubSerializer
}
