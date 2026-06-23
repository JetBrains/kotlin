/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.factory

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.*
import com.intellij.util.io.StringRef
import org.jetbrains.kotlin.psi.KtContextReceiver
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementType
import org.jetbrains.kotlin.psi.stubs.impl.KotlinContextReceiverStubImpl

@Suppress("UnstableApiUsage") // KT-78356: the platform stub-decoupling API is still @ApiStatus.Experimental
internal object KotlinContextReceiverStubFactory : StubElementFactory<KotlinContextReceiverStubImpl, KtContextReceiver> {
    @OptIn(KtImplementationDetail::class)
    override fun shouldCreateStub(node: ASTNode): Boolean = KtStubElementType.shouldCreateStubDependingOnParent(node)

    override fun createStub(
        psi: KtContextReceiver,
        parentStub: StubElement<out PsiElement>?,
    ): KotlinContextReceiverStubImpl = KotlinContextReceiverStubImpl(
        parent = parentStub,
        labelRef = StringRef.fromString(psi.labelName()),
    )

    override fun createPsi(stub: KotlinContextReceiverStubImpl): KtContextReceiver = KtContextReceiver(stub)
}

internal object KotlinContextReceiverStubSerializer : StubSerializer<KotlinContextReceiverStubImpl> {
    override fun getExternalId(): String = "kotlin.CONTEXT_RECEIVER"

    override fun serialize(stub: KotlinContextReceiverStubImpl, dataStream: StubOutputStream) {
        dataStream.writeName(stub.label)
    }

    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?): KotlinContextReceiverStubImpl {
        return KotlinContextReceiverStubImpl(parentStub, dataStream.readName())
    }

    override fun indexStub(stub: KotlinContextReceiverStubImpl, sink: IndexSink) {
        // not indexed
    }
}
