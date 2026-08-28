/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.impl

import com.intellij.psi.stubs.StubElement
import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.psi.KtElementImplStub
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.psi.stubs.KotlinPlaceHolderStub
import org.jetbrains.kotlin.psi.stubs.KotlinStubElement

@KtImplementationDetail
open class KotlinPlaceHolderStubImpl<T : KtElementImplStub<*>>(
    parent: StubElement<*>?,
    elementType: IElementType,
) : KotlinStubBaseImpl<T>(parent, elementType), KotlinPlaceHolderStub<T> {
    @KtImplementationDetail
    override fun copyInto(newParent: StubElement<*>?): KotlinPlaceHolderStubImpl<T> = KotlinPlaceHolderStubImpl(
        parent = newParent,
        elementType = elementType,
    )

    @KtImplementationDetail
    override fun isEquivalentTo(other: KotlinStubElement<*>): Boolean =
        other.javaClass == javaClass &&
                other.elementType == elementType
}
