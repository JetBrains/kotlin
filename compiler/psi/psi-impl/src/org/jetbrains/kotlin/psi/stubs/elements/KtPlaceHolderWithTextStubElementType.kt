/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.util.ArrayFactory
import org.jetbrains.annotations.NonNls
import org.jetbrains.kotlin.psi.KtElementImplStub
import org.jetbrains.kotlin.psi.stubs.KotlinPlaceHolderWithTextStub
import org.jetbrains.kotlin.psi.stubs.impl.KotlinPlaceHolderWithTextStubImpl
import java.util.function.Function

class KtPlaceHolderWithTextStubElementType<T : KtElementImplStub<*>>(
    @NonNls debugName: String,
    psiFromAstFactory: Function<ASTNode, T>,
    psiFromStubFactory: Function<in KotlinPlaceHolderWithTextStubImpl<T>, out T>,
    arrayFactory: ArrayFactory<T>,
) : KtStubElementType<KotlinPlaceHolderWithTextStubImpl<T>, T>(
    debugName,
    psiFromAstFactory,
    @Suppress("UNCHECKED_CAST") (psiFromStubFactory as Function<KotlinPlaceHolderWithTextStubImpl<T>, T>),
    arrayFactory,
    false,
) {

    override fun createStub(psi: T, parentStub: StubElement<*>): KotlinPlaceHolderWithTextStubImpl<T> {
        return KotlinPlaceHolderWithTextStubImpl(parentStub, this, psi.text)
    }

    override fun serialize(stub: KotlinPlaceHolderWithTextStubImpl<T>, dataStream: StubOutputStream) {
        dataStream.writeUTFFast(stub.text)
    }

    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>): KotlinPlaceHolderWithTextStubImpl<T> {
        val text = dataStream.readUTFFast()
        return KotlinPlaceHolderWithTextStubImpl(parentStub, this, text)
    }
}
