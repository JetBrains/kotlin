/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.impl

import com.intellij.psi.stubs.StubElement
import com.intellij.util.io.StringRef
import org.jetbrains.kotlin.psi.KtContextReceiver
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.psi.stubs.KotlinContextReceiverStub
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes

@OptIn(KtImplementationDetail::class)
class KotlinContextReceiverStubImpl(
    parent: StubElement<*>?,
    private val labelRef: StringRef?,
) : KotlinStubBaseImpl<KtContextReceiver>(
    parent = parent,
    elementType = KtStubElementTypes.CONTEXT_RECEIVER,
), KotlinContextReceiverStub {
    override val label: String? get() = labelRef?.string

    @KtImplementationDetail
    override fun copyInto(newParent: StubElement<*>?): KotlinContextReceiverStubImpl = KotlinContextReceiverStubImpl(
        parent = newParent,
        labelRef = labelRef,
    )
}
