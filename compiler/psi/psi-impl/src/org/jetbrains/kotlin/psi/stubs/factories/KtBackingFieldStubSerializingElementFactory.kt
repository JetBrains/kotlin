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
import org.jetbrains.kotlin.psi.KtBackingField
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.psi.stubs.impl.KotlinBackingFieldStubImpl

internal object KtBackingFieldStubSerializingElementFactory :
    KtStubSerializingElementFactory<KotlinBackingFieldStubImpl, KtBackingField>(
        type = KtNodeTypes.BACKING_FIELD,
    ) {

    override fun createPsi(stub: KotlinBackingFieldStubImpl): KtBackingField = KtBackingField(stub)

    override fun createStub(
        psi: KtBackingField,
        parentStub: StubElement<*>?,
    ): KotlinBackingFieldStubImpl = KotlinBackingFieldStubImpl(
        /* parent = */ parentStub,
        /* hasInitializer = */ psi.hasInitializer(),
    )

    override fun serialize(stub: KotlinBackingFieldStubImpl, dataStream: StubOutputStream) {
        dataStream.writeBoolean(stub.hasInitializer)
    }

    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?): KotlinBackingFieldStubImpl {
        val hasInitializer = dataStream.readBoolean()
        return KotlinBackingFieldStubImpl(parentStub, hasInitializer)
    }
}
