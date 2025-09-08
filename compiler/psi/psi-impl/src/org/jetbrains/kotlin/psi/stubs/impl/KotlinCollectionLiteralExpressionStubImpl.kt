/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.impl

import com.intellij.psi.stubs.StubElement
import org.jetbrains.kotlin.psi.KtCollectionLiteralExpression
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.psi.stubs.KotlinCollectionLiteralExpressionStub
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes

@OptIn(KtImplementationDetail::class)
class KotlinCollectionLiteralExpressionStubImpl(
    parent: StubElement<*>?,
    override val innerExpressionCount: Int,
) : KotlinStubBaseImpl<KtCollectionLiteralExpression>(parent, KtStubElementTypes.COLLECTION_LITERAL_EXPRESSION),
    KotlinCollectionLiteralExpressionStub {
    @KtImplementationDetail
    override fun copyInto(
        newParent: StubElement<*>?,
    ): KotlinCollectionLiteralExpressionStubImpl = KotlinCollectionLiteralExpressionStubImpl(
        parent = newParent,
        innerExpressionCount = innerExpressionCount,
    )
}
