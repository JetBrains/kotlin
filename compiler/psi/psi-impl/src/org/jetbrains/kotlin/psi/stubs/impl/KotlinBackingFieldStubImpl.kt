/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.impl

import com.intellij.psi.stubs.StubElement
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.psi.KtBackingField
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.psi.stubs.KotlinBackingFieldStub
import org.jetbrains.kotlin.psi.stubs.KotlinStubElement

@OptIn(KtImplementationDetail::class)
class KotlinBackingFieldStubImpl(
    parent: StubElement<*>?,
    override val hasInitializer: Boolean,
) : KotlinStubBaseImpl<KtBackingField>(parent, KtNodeTypes.BACKING_FIELD), KotlinBackingFieldStub {
    @KtImplementationDetail
    override fun copyInto(newParent: StubElement<*>?): KotlinBackingFieldStubImpl = KotlinBackingFieldStubImpl(
        parent = newParent,
        hasInitializer = hasInitializer,
    )

    @KtImplementationDetail
    override fun isEquivalentTo(other: KotlinStubElement<*>): Boolean =
        other is KotlinBackingFieldStubImpl &&
                other.hasInitializer == hasInitializer
}
