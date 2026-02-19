/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.impl

import com.intellij.psi.stubs.StubElement
import com.intellij.util.io.StringRef
import org.jetbrains.kotlin.psi.KtAnnotationUseSiteTarget
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.psi.stubs.KotlinAnnotationUseSiteTargetStub
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes

@OptIn(KtImplementationDetail::class)
class KotlinAnnotationUseSiteTargetStubImpl(
    parent: StubElement<*>?,
    private val useSiteTargetRef: StringRef,
) : KotlinStubBaseImpl<KtAnnotationUseSiteTarget>(parent, KtStubElementTypes.ANNOTATION_TARGET), KotlinAnnotationUseSiteTargetStub {
    override val useSiteTarget: String
        get() = useSiteTargetRef.string

    @KtImplementationDetail
    override fun copyInto(newParent: StubElement<*>?): KotlinAnnotationUseSiteTargetStubImpl = KotlinAnnotationUseSiteTargetStubImpl(
        parent = newParent,
        useSiteTargetRef = useSiteTargetRef,
    )
}
