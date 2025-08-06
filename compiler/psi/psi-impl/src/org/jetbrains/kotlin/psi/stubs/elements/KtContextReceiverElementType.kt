/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.elements

import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.util.io.StringRef
import org.jetbrains.kotlin.psi.KtContextReceiver
import org.jetbrains.kotlin.psi.stubs.KotlinContextReceiverStub
import org.jetbrains.kotlin.psi.stubs.impl.KotlinContextReceiverStubImpl

class KtContextReceiverElementType(debugName: String) : KtStubElementType<KotlinContextReceiverStubImpl, KtContextReceiver>(
    debugName,
    KtContextReceiver::class.java,
    KotlinContextReceiverStub::class.java,
) {
    override fun createStub(
        element: KtContextReceiver,
        parentStub: StubElement<*>?,
    ): KotlinContextReceiverStubImpl = KotlinContextReceiverStubImpl(
        parent = parentStub,
        elementType = this,
        _label = StringRef.fromString(element.labelName()),
    )

    override fun serialize(stub: KotlinContextReceiverStubImpl, dataStream: StubOutputStream) =
        dataStream.writeName(stub.label)

    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?) =
        KotlinContextReceiverStubImpl(parentStub, this, dataStream.readName())
}
