/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.factory

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.*
import org.jetbrains.kotlin.psi.KtContractEffect
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementType
import org.jetbrains.kotlin.psi.stubs.impl.KotlinContractEffectStubImpl

@Suppress("UnstableApiUsage") // KT-78356: the platform stub-decoupling API is still @ApiStatus.Experimental
internal object KotlinContractEffectStubFactory : StubElementFactory<KotlinContractEffectStubImpl, KtContractEffect> {
    @OptIn(KtImplementationDetail::class)
    override fun shouldCreateStub(node: ASTNode): Boolean = KtStubElementType.shouldCreateStubDependingOnParent(node)

    override fun createStub(psi: KtContractEffect, parentStub: StubElement<out PsiElement>?): KotlinContractEffectStubImpl {
        return KotlinContractEffectStubImpl(parentStub)
    }

    override fun createPsi(stub: KotlinContractEffectStubImpl): KtContractEffect = KtContractEffect(stub)
}

internal object KotlinContractEffectStubSerializer : StubSerializer<KotlinContractEffectStubImpl> {
    override fun getExternalId(): String = "kotlin.CONTRACT_EFFECT"

    override fun serialize(stub: KotlinContractEffectStubImpl, dataStream: StubOutputStream) {
    }

    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?): KotlinContractEffectStubImpl {
        return KotlinContractEffectStubImpl(parentStub)
    }

    override fun indexStub(stub: KotlinContractEffectStubImpl, sink: IndexSink) {
        // not indexed
    }
}
