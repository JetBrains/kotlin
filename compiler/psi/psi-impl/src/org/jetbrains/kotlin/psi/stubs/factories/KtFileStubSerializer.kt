/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(KtImplementationDetail::class)

package org.jetbrains.kotlin.psi.stubs.factories

import com.intellij.psi.stubs.*
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.psi.stubs.elements.KtFileElementType
import org.jetbrains.kotlin.psi.stubs.elements.StubIndexService
import org.jetbrains.kotlin.psi.stubs.impl.KotlinFileStubImpl
import org.jetbrains.kotlin.psi.stubs.impl.KotlinFileStubKindImpl

internal object KtFileStubSerializer : StubSerializer<KotlinFileStubImpl> {
    override fun getExternalId(): String = KtFileElementType.NAME

    override fun serialize(stub: KotlinFileStubImpl, dataStream: StubOutputStream) {
        KotlinFileStubKindImpl.serialize(stub.kind, dataStream)
    }

    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?): KotlinFileStubImpl {
        val kind = KotlinFileStubKindImpl.deserialize(dataStream)
        return KotlinFileStubImpl(file = null, kind = kind)
    }

    override fun indexStub(stub: KotlinFileStubImpl, sink: IndexSink) {
        StubIndexService.getInstance().indexFile(stub, sink)
    }
}
