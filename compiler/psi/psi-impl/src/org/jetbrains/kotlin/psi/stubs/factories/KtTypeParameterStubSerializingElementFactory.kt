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
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.psi.KtTypeParameter
import org.jetbrains.kotlin.psi.stubs.impl.KotlinTypeParameterStubImpl

internal object KtTypeParameterStubSerializingElementFactory :
    KtStubSerializingElementFactory<KotlinTypeParameterStubImpl, KtTypeParameter>(
        type = KtNodeTypes.TYPE_PARAMETER,
    ) {

    override fun createPsi(stub: KotlinTypeParameterStubImpl): KtTypeParameter = KtTypeParameter(stub)

    override fun createStub(
        psi: KtTypeParameter,
        parentStub: StubElement<*>?,
    ): KotlinTypeParameterStubImpl = KotlinTypeParameterStubImpl(
        parent = parentStub,
        name = StringRef.fromString(psi.name),
    )

    override fun serialize(stub: KotlinTypeParameterStubImpl, dataStream: StubOutputStream) {
        dataStream.writeName(stub.getName())
    }

    override fun deserialize(
        dataStream: StubInputStream,
        parentStub: StubElement<*>?,
    ): KotlinTypeParameterStubImpl = KotlinTypeParameterStubImpl(
        parent = parentStub,
        name = dataStream.readName(),
    )
}
