/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.elements

import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import org.jetbrains.annotations.NonNls
import org.jetbrains.kotlin.psi.KtFunctionType
import org.jetbrains.kotlin.psi.stubs.KotlinFunctionTypeStub
import org.jetbrains.kotlin.psi.stubs.impl.KotlinFunctionTypeStubImpl

class KtFunctionTypeElementType(@NonNls debugName: String) : KtStubElementType<KotlinFunctionTypeStub, KtFunctionType>(
    debugName,
    KtFunctionType::class.java,
    KotlinFunctionTypeStub::class.java,
) {
    override fun createStub(psi: KtFunctionType, parentStub: StubElement<out PsiElement?>?): KotlinFunctionTypeStub =
        KotlinFunctionTypeStubImpl(parentStub)

    override fun serialize(stub: KotlinFunctionTypeStub, dataStream: StubOutputStream) {
        require(stub is KotlinFunctionTypeStubImpl)
        serializeTypeBean(dataStream, stub.abbreviatedType)
    }

    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?): KotlinFunctionTypeStub {
        val abbreviatedType = deserializeClassTypeBean(dataStream)
        return KotlinFunctionTypeStubImpl(parentStub, abbreviatedType)
    }
}
