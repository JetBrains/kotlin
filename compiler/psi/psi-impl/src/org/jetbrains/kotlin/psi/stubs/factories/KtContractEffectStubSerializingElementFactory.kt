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
import org.jetbrains.kotlin.psi.KtContractEffect
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.psi.stubs.impl.KotlinContractEffectStubImpl

internal object KtContractEffectStubSerializingElementFactory :
    KtStubSerializingElementFactory<KotlinContractEffectStubImpl, KtContractEffect>(
        type = KtNodeTypes.CONTRACT_EFFECT,
    ) {

    override fun createPsi(stub: KotlinContractEffectStubImpl): KtContractEffect = KtContractEffect(stub)

    override fun createStub(
        psi: KtContractEffect,
        parentStub: StubElement<*>?,
    ): KotlinContractEffectStubImpl = KotlinContractEffectStubImpl(parentStub)

    override fun serialize(stub: KotlinContractEffectStubImpl, dataStream: StubOutputStream) {
        // there is nothing to serialize
    }

    override fun deserialize(
        dataStream: StubInputStream,
        parentStub: StubElement<*>?,
    ): KotlinContractEffectStubImpl = KotlinContractEffectStubImpl(parentStub)
}
