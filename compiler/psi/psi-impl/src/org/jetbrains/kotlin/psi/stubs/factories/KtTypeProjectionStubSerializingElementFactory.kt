/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:OptIn(KtImplementationDetail::class)

package org.jetbrains.kotlin.psi.stubs.factories

import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.psi.KtTypeProjection
import org.jetbrains.kotlin.psi.stubs.impl.KotlinTypeProjectionStubImpl

internal object KtTypeProjectionStubSerializingElementFactory :
    KtStubSerializingElementFactory<KotlinTypeProjectionStubImpl, KtTypeProjection>(
        type = KtNodeTypes.TYPE_PROJECTION,
    ) {

    override fun createPsi(stub: KotlinTypeProjectionStubImpl): KtTypeProjection = KtTypeProjection(stub)

    override fun createStub(
        psi: KtTypeProjection,
        parentStub: StubElement<*>?,
    ): KotlinTypeProjectionStubImpl = KotlinTypeProjectionStubImpl(parentStub, psi.projectionKind.ordinal)

    override fun serialize(stub: KotlinTypeProjectionStubImpl, dataStream: StubOutputStream) {
        dataStream.writeVarInt(stub.projectionKind.ordinal)
    }

    override fun deserialize(
        dataStream: StubInputStream,
        parentStub: StubElement<*>?,
    ): KotlinTypeProjectionStubImpl = KotlinTypeProjectionStubImpl(
        parentStub,
        /* projectionKindOrdinal = */ dataStream.readVarInt(),
    )
}
