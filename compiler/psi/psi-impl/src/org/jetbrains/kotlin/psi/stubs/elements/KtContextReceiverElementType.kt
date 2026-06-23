/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("UnstableApiUsage") // KT-78356: the platform stub-decoupling API is still @ApiStatus.Experimental

package org.jetbrains.kotlin.psi.stubs.elements

import com.intellij.psi.stubs.StubElementFactory
import com.intellij.psi.stubs.StubSerializer
import org.jetbrains.kotlin.psi.KtContextReceiver
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.psi.stubs.KotlinContextReceiverStub
import org.jetbrains.kotlin.psi.stubs.factory.KotlinContextReceiverStubFactory
import org.jetbrains.kotlin.psi.stubs.factory.KotlinContextReceiverStubSerializer
import org.jetbrains.kotlin.psi.stubs.impl.KotlinContextReceiverStubImpl

class KtContextReceiverElementType(debugName: String) : KtStubElementType<KotlinContextReceiverStubImpl, KtContextReceiver>(
    debugName,
    KtContextReceiver::class.java,
    KotlinContextReceiverStub::class.java,
) {
    @KtImplementationDetail
    override fun getStubFactory(): StubElementFactory<KotlinContextReceiverStubImpl, KtContextReceiver> =
        KotlinContextReceiverStubFactory

    @KtImplementationDetail
    override fun getStubSerializer(): StubSerializer<KotlinContextReceiverStubImpl> = KotlinContextReceiverStubSerializer
}
