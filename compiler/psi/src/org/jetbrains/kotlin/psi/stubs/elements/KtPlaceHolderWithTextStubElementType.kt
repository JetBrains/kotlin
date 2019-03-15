/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.elements

import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import org.jetbrains.annotations.NonNls
import org.jetbrains.kotlin.psi.KtElementImplStub
import org.jetbrains.kotlin.psi.stubs.KotlinPlaceHolderWithTextStub
import org.jetbrains.kotlin.psi.stubs.impl.KotlinPlaceHolderWithTextStubImpl

class KtPlaceHolderWithTextStubElementType<T : KtElementImplStub<out StubElement<*>>>(@NonNls debugName: String, psiClass: Class<T>) :
    KtStubElementType<KotlinPlaceHolderWithTextStub<T>, T>(debugName, psiClass, KotlinPlaceHolderWithTextStub::class.java) {

    override fun createStub(psi: T, parentStub: StubElement<*>): KotlinPlaceHolderWithTextStub<T> {
        return KotlinPlaceHolderWithTextStubImpl(parentStub, this, psi.text)
    }

    override fun serialize(stub: KotlinPlaceHolderWithTextStub<T>, dataStream: StubOutputStream) {
        dataStream.writeUTFFast(stub.text())
    }

    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>): KotlinPlaceHolderWithTextStub<T> {
        val text = dataStream.readUTFFast()
        return KotlinPlaceHolderWithTextStubImpl(parentStub, this, text)
    }
}
