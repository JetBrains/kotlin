/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.impl

import com.intellij.psi.stubs.StubElement
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.psi.KtFunctionType
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.psi.stubs.KotlinFunctionTypeStub
import org.jetbrains.kotlin.psi.stubs.KotlinStubElement

/**
 * @param abbreviatedType The type alias application from which this type was originally expanded. It can be used to render or navigate to
 *  the original type alias instead of the expanded type.
 */
@OptIn(KtImplementationDetail::class)
class KotlinFunctionTypeStubImpl(
    parent: StubElement<*>?,
    val abbreviatedType: KotlinClassTypeBean?,
) : KotlinStubBaseImpl<KtFunctionType>(parent, KtNodeTypes.FUNCTION_TYPE), KotlinFunctionTypeStub {
    @KtImplementationDetail
    override fun copyInto(newParent: StubElement<*>?): KotlinFunctionTypeStubImpl = KotlinFunctionTypeStubImpl(
        parent = newParent,
        abbreviatedType = abbreviatedType,
    )

    @KtImplementationDetail
    override fun isEquivalentTo(other: KotlinStubElement<*>): Boolean =
        other is KotlinFunctionTypeStubImpl &&
                other.abbreviatedType == abbreviatedType
}
