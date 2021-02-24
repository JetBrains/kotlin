/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.elements

import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import org.jetbrains.kotlin.psi.KtContractEffect
import org.jetbrains.kotlin.psi.stubs.KotlinContractEffectStub
import org.jetbrains.kotlin.psi.stubs.impl.KotlinContractEffectStubImpl

class KtContractEffectElementType(debugName: String, psiClass: Class<KtContractEffect>) :
    KtStubElementType<KotlinContractEffectStub, KtContractEffect>(debugName, psiClass, KotlinContractEffectStub::class.java) {
    override fun serialize(stub: KotlinContractEffectStub, dataStream: StubOutputStream) {
    }

    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<PsiElement>?): KotlinContractEffectStub {
        return KotlinContractEffectStubImpl(parentStub, this)
    }

    override fun createStub(psi: KtContractEffect, parentStub: StubElement<PsiElement>?): KotlinContractEffectStub {
        return KotlinContractEffectStubImpl(parentStub, this)
    }
}