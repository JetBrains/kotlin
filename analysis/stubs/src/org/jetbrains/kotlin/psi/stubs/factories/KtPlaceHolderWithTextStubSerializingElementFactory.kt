/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.factories

import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.psi.KtElementImplStub
import org.jetbrains.kotlin.psi.stubs.KotlinPlaceHolderWithTextStub
import org.jetbrains.kotlin.psi.stubs.impl.KotlinPlaceHolderWithTextStubImpl

/**
 * A factory for elements whose stub carries only their source text.
 */
internal class KtPlaceHolderWithTextStubSerializingElementFactory<Psi : KtElementImplStub<out StubElement<*>>>(
    type: IElementType,
    private val psiFactory: (KotlinPlaceHolderWithTextStub<Psi>) -> Psi,
) : KtStubSerializingElementFactory<KotlinPlaceHolderWithTextStubImpl<Psi>, Psi>(type) {

    override fun createPsi(stub: KotlinPlaceHolderWithTextStubImpl<Psi>): Psi = psiFactory(stub)

    override fun createStub(
        psi: Psi,
        parentStub: StubElement<*>?,
    ): KotlinPlaceHolderWithTextStubImpl<Psi> = KotlinPlaceHolderWithTextStubImpl(
        parent = parentStub,
        elementType = type,
        text = psi.text,
    )

    override fun serialize(stub: KotlinPlaceHolderWithTextStubImpl<Psi>, dataStream: StubOutputStream) {
        dataStream.writeUTFFast(stub.text)
    }

    override fun deserialize(
        dataStream: StubInputStream,
        parentStub: StubElement<*>?,
    ): KotlinPlaceHolderWithTextStubImpl<Psi> = KotlinPlaceHolderWithTextStubImpl(
        parent = parentStub,
        elementType = type,
        text = dataStream.readUTFFast(),
    )
}
