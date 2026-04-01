/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.util.ArrayFactory
import org.jetbrains.kotlin.psi.KtContractEffect
import org.jetbrains.kotlin.psi.stubs.KotlinContractEffectStub
import org.jetbrains.kotlin.psi.stubs.impl.KotlinContractEffectStubImpl
import java.util.function.Function

class KtContractEffectElementType(
    debugName: String,
    psiFromAstFactory: Function<ASTNode, KtContractEffect>,
    psiFromStubFactory: Function<KotlinContractEffectStub, KtContractEffect>,
    arrayFactory: ArrayFactory<KtContractEffect>,
) : KtStubElementType<KotlinContractEffectStubImpl, KtContractEffect>(
    debugName,
    psiFromAstFactory,
    @Suppress("UNCHECKED_CAST") (psiFromStubFactory as Function<KotlinContractEffectStubImpl, KtContractEffect>),
    arrayFactory,
    false,
) {
    override fun serialize(stub: KotlinContractEffectStubImpl, dataStream: StubOutputStream) {
    }

    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<PsiElement>?): KotlinContractEffectStubImpl {
        return KotlinContractEffectStubImpl(parentStub)
    }

    override fun createStub(psi: KtContractEffect, parentStub: StubElement<*>?): KotlinContractEffectStubImpl {
        return KotlinContractEffectStubImpl(parentStub)
    }
}
