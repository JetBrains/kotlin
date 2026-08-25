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
import org.jetbrains.kotlin.psi.stubs.KotlinPlaceHolderStub
import org.jetbrains.kotlin.psi.stubs.impl.KotlinPlaceHolderStubImpl

/**
 * A factory for elements whose stub carries no data beyond its own presence.
 */
internal open class KtPlaceHolderStubSerializingElementFactory<Psi : KtElementImplStub<out StubElement<*>>>(
    type: IElementType,
    private val psiFactory: (KotlinPlaceHolderStub<Psi>) -> Psi,
) : KtStubSerializingElementFactory<KotlinPlaceHolderStubImpl<Psi>, Psi>(type) {

    override fun createPsi(stub: KotlinPlaceHolderStubImpl<Psi>): Psi = psiFactory(stub)

    override fun createStub(
        psi: Psi,
        parentStub: StubElement<*>?,
    ): KotlinPlaceHolderStubImpl<Psi> = KotlinPlaceHolderStubImpl(parentStub, type)

    override fun serialize(stub: KotlinPlaceHolderStubImpl<Psi>, dataStream: StubOutputStream) {
        // there is nothing to serialize
    }

    override fun deserialize(
        dataStream: StubInputStream,
        parentStub: StubElement<*>?,
    ): KotlinPlaceHolderStubImpl<Psi> = KotlinPlaceHolderStubImpl(parentStub, type)
}
