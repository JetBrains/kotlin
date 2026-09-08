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
import org.jetbrains.kotlin.psi.KtFunctionType
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.psi.stubs.elements.deserializeClassTypeBean
import org.jetbrains.kotlin.psi.stubs.elements.serializeTypeBean
import org.jetbrains.kotlin.psi.stubs.impl.KotlinFunctionTypeStubImpl

internal object KtFunctionTypeStubSerializingElementFactory :
    KtStubSerializingElementFactory<KotlinFunctionTypeStubImpl, KtFunctionType>(
        type = KtNodeTypes.FUNCTION_TYPE,
    ) {

    override fun createPsi(stub: KotlinFunctionTypeStubImpl): KtFunctionType = KtFunctionType(stub)

    override fun createStub(
        psi: KtFunctionType,
        parentStub: StubElement<*>?,
    ): KotlinFunctionTypeStubImpl = KotlinFunctionTypeStubImpl(parent = parentStub, abbreviatedType = null)

    override fun serialize(stub: KotlinFunctionTypeStubImpl, dataStream: StubOutputStream) {
        serializeTypeBean(dataStream, stub.abbreviatedType)
    }

    override fun deserialize(
        dataStream: StubInputStream,
        parentStub: StubElement<*>?,
    ): KotlinFunctionTypeStubImpl = KotlinFunctionTypeStubImpl(
        parent = parentStub,
        abbreviatedType = deserializeClassTypeBean(dataStream),
    )
}
