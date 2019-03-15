/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.elements

import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.kotlin.psi.stubs.KotlinValueArgumentStub
import org.jetbrains.kotlin.psi.stubs.impl.KotlinValueArgumentStubImpl

class KtValueArgumentElementType<T : KtValueArgument>(debugName: String, psiClass: Class<T>) :
    KtStubElementType<KotlinValueArgumentStub<T>, T>(debugName, psiClass, KotlinValueArgumentStub::class.java) {

    override fun createStub(psi: T, parentStub: StubElement<PsiElement>?): KotlinValueArgumentStub<T> {
        return KotlinValueArgumentStubImpl(parentStub, this, psi.isSpread)
    }

    override fun serialize(stub: KotlinValueArgumentStub<T>, dataStream: StubOutputStream) {
        dataStream.writeBoolean(stub.isSpread())
    }

    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<PsiElement>?): KotlinValueArgumentStub<T> {
        val isSpread = dataStream.readBoolean()
        return KotlinValueArgumentStubImpl(parentStub, this, isSpread)
    }
}
