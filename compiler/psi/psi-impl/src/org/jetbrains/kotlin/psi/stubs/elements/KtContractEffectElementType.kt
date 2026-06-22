/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("UnstableApiUsage") // KT-78356: the platform stub-decoupling API is still @ApiStatus.Experimental

package org.jetbrains.kotlin.psi.stubs.elements

import com.intellij.psi.stubs.StubElementFactory
import com.intellij.psi.stubs.StubSerializer
import org.jetbrains.kotlin.psi.KtContractEffect
import org.jetbrains.kotlin.psi.stubs.KotlinContractEffectStub
import org.jetbrains.kotlin.psi.stubs.factory.KotlinContractEffectStubFactory
import org.jetbrains.kotlin.psi.stubs.factory.KotlinContractEffectStubSerializer
import org.jetbrains.kotlin.psi.stubs.impl.KotlinContractEffectStubImpl

class KtContractEffectElementType(debugName: String, psiClass: Class<KtContractEffect>) :
    KtStubElementType<KotlinContractEffectStubImpl, KtContractEffect>(
        debugName,
        psiClass,
        KotlinContractEffectStub::class.java,
    ) {
    override fun getStubFactory(): StubElementFactory<KotlinContractEffectStubImpl, KtContractEffect> =
        KotlinContractEffectStubFactory

    override fun getStubSerializer(): StubSerializer<KotlinContractEffectStubImpl> = KotlinContractEffectStubSerializer
}
