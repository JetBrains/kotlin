/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.psi.stubs.impl

import com.intellij.psi.stubs.StubElement
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.psi.KtPropertyAccessor
import org.jetbrains.kotlin.psi.stubs.KotlinPropertyAccessorStub
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes

@OptIn(KtImplementationDetail::class)
class KotlinPropertyAccessorStubImpl(
    parent: StubElement<*>?,
    override val isGetter: Boolean,
    override val hasBody: Boolean,
    override val hasNoExpressionBody: Boolean,
    override val mayHaveContract: Boolean,
) : KotlinStubBaseImpl<KtPropertyAccessor>(parent, KtStubElementTypes.PROPERTY_ACCESSOR), KotlinPropertyAccessorStub {
    @KtImplementationDetail
    override fun copyInto(newParent: StubElement<*>?): KotlinPropertyAccessorStubImpl = KotlinPropertyAccessorStubImpl(
        parent = newParent,
        isGetter = isGetter,
        hasBody = hasBody,
        hasNoExpressionBody = hasNoExpressionBody,
        mayHaveContract = mayHaveContract,
    )
}
