/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.factories

import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.kotlin.psi.stubs.KotlinValueArgumentStub
import org.jetbrains.kotlin.psi.stubs.impl.KotlinValueArgumentStubImpl

/**
 * A factory for elements which represent an argument of a call.
 */
internal class KtValueArgumentStubSerializingElementFactory<Psi : KtValueArgument>(
    type: IElementType,
    private val psiFactory: (KotlinValueArgumentStub<Psi>) -> Psi,
) : KtStubSerializingElementFactory<KotlinValueArgumentStubImpl<Psi>, Psi>(type) {

    override fun createPsi(stub: KotlinValueArgumentStubImpl<Psi>): Psi = psiFactory(stub)

    override fun createStub(
        psi: Psi,
        parentStub: StubElement<*>?,
    ): KotlinValueArgumentStubImpl<Psi> = KotlinValueArgumentStubImpl(
        parent = parentStub,
        elementType = type,
        isSpread = psi.isSpread,
    )

    override fun serialize(stub: KotlinValueArgumentStubImpl<Psi>, dataStream: StubOutputStream) {
        dataStream.writeBoolean(stub.isSpread)
    }

    override fun deserialize(
        dataStream: StubInputStream,
        parentStub: StubElement<*>?,
    ): KotlinValueArgumentStubImpl<Psi> = KotlinValueArgumentStubImpl(
        parent = parentStub,
        elementType = type,
        isSpread = dataStream.readBoolean(),
    )
}
