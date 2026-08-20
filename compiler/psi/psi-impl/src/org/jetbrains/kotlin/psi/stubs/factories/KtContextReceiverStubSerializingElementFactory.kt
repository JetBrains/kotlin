/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:OptIn(KtImplementationDetail::class)

package org.jetbrains.kotlin.psi.stubs.factories

import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.util.io.StringRef
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.psi.KtContextReceiver
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.psi.stubs.impl.KotlinContextReceiverStubImpl

internal object KtContextReceiverStubSerializingElementFactory :
    KtStubSerializingElementFactory<KotlinContextReceiverStubImpl, KtContextReceiver>(
        type = KtNodeTypes.CONTEXT_RECEIVER,
    ) {

    override fun createPsi(stub: KotlinContextReceiverStubImpl): KtContextReceiver = KtContextReceiver(stub)

    override fun createStub(
        psi: KtContextReceiver,
        parentStub: StubElement<*>?,
    ): KotlinContextReceiverStubImpl = KotlinContextReceiverStubImpl(
        parent = parentStub,
        labelRef = StringRef.fromString(psi.labelName()),
    )

    override fun serialize(stub: KotlinContextReceiverStubImpl, dataStream: StubOutputStream) {
        dataStream.writeName(stub.label)
    }

    override fun deserialize(
        dataStream: StubInputStream,
        parentStub: StubElement<*>?,
    ): KotlinContextReceiverStubImpl = KotlinContextReceiverStubImpl(
        parent = parentStub,
        labelRef = dataStream.readName(),
    )
}
