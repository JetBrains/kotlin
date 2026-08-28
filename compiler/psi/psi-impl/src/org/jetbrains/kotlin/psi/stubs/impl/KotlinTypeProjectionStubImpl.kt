/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.impl

import com.intellij.psi.stubs.StubElement
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.psi.KtProjectionKind
import org.jetbrains.kotlin.psi.KtTypeProjection
import org.jetbrains.kotlin.psi.stubs.KotlinStubElement
import org.jetbrains.kotlin.psi.stubs.KotlinTypeProjectionStub

@KtImplementationDetail
class KotlinTypeProjectionStubImpl(
    parent: StubElement<*>?,
    private val projectionKindOrdinal: Int,
) : KotlinStubBaseImpl<KtTypeProjection>(parent, KtNodeTypes.TYPE_PROJECTION), KotlinTypeProjectionStub {
    override val projectionKind: KtProjectionKind
        get() = KtProjectionKind.entries[projectionKindOrdinal]

    @KtImplementationDetail
    override fun copyInto(newParent: StubElement<*>?): KotlinTypeProjectionStubImpl = KotlinTypeProjectionStubImpl(
        parent = newParent,
        projectionKindOrdinal = projectionKindOrdinal,
    )

    @KtImplementationDetail
    override fun isEquivalentTo(other: KotlinStubElement<*>): Boolean =
        other is KotlinTypeProjectionStubImpl &&
                other.projectionKindOrdinal == projectionKindOrdinal
}
