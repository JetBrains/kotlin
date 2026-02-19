/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.impl

import com.intellij.psi.stubs.StubElement
import org.jetbrains.kotlin.psi.KtClassLiteralExpression
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.psi.stubs.KotlinClassLiteralExpressionStub
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes

@OptIn(KtImplementationDetail::class)
class KotlinClassLiteralExpressionStubImpl(
    parent: StubElement<*>?,
) : KotlinStubBaseImpl<KtClassLiteralExpression>(parent, KtStubElementTypes.CLASS_LITERAL_EXPRESSION), KotlinClassLiteralExpressionStub {
    @KtImplementationDetail
    override fun copyInto(newParent: StubElement<*>?): KotlinClassLiteralExpressionStubImpl = KotlinClassLiteralExpressionStubImpl(
        parent = newParent,
    )
}