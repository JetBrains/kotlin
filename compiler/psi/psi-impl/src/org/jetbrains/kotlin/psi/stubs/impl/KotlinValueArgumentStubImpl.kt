/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.impl

import com.intellij.psi.stubs.StubElement
import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.kotlin.psi.stubs.KotlinStubElement
import org.jetbrains.kotlin.psi.stubs.KotlinValueArgumentStub

@OptIn(KtImplementationDetail::class)
class KotlinValueArgumentStubImpl<T : KtValueArgument>(
    parent: StubElement<*>?,
    elementType: IElementType,
    override val isSpread: Boolean
) : KotlinPlaceHolderStubImpl<T>(parent, elementType), KotlinValueArgumentStub<T> {
    @KtImplementationDetail
    override fun copyInto(newParent: StubElement<*>?): KotlinValueArgumentStubImpl<T> = KotlinValueArgumentStubImpl(
        parent = newParent,
        elementType = elementType,
        isSpread = isSpread,
    )

    @KtImplementationDetail
    override fun isEquivalentTo(other: KotlinStubElement<*>): Boolean =
        other is KotlinValueArgumentStubImpl<*> &&
                other.isSpread == isSpread &&
                super.isEquivalentTo(other)
}
