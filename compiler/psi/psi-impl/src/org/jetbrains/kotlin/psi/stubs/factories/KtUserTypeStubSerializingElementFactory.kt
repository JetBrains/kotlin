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
import org.jetbrains.kotlin.psi.KtUserType
import org.jetbrains.kotlin.psi.stubs.elements.deserializeClassTypeBean
import org.jetbrains.kotlin.psi.stubs.elements.deserializeTypeBean
import org.jetbrains.kotlin.psi.stubs.elements.serializeTypeBean
import org.jetbrains.kotlin.psi.stubs.impl.KotlinUserTypeStubImpl

internal object KtUserTypeStubSerializingElementFactory :
    KtStubSerializingElementFactory<KotlinUserTypeStubImpl, KtUserType>(
        type = KtNodeTypes.USER_TYPE,
    ) {

    override fun createPsi(stub: KotlinUserTypeStubImpl): KtUserType = KtUserType(stub)

    override fun createStub(
        psi: KtUserType,
        parentStub: StubElement<*>?,
    ): KotlinUserTypeStubImpl = KotlinUserTypeStubImpl(
        parent = parentStub,
        upperBound = null,
        abbreviatedType = null,
    )

    override fun serialize(stub: KotlinUserTypeStubImpl, dataStream: StubOutputStream) {
        serializeTypeBean(dataStream, stub.upperBound)
        serializeTypeBean(dataStream, stub.abbreviatedType)
    }

    override fun deserialize(
        dataStream: StubInputStream,
        parentStub: StubElement<*>?,
    ): KotlinUserTypeStubImpl = KotlinUserTypeStubImpl(
        parent = parentStub,
        upperBound = deserializeTypeBean(dataStream),
        abbreviatedType = deserializeClassTypeBean(dataStream),
    )
}
