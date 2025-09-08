/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi

import com.intellij.lang.ASTNode
import org.jetbrains.kotlin.KtStubBasedElementTypes
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType
import org.jetbrains.kotlin.psi.stubs.KotlinContractEffectStub

class KtContractEffect: KtElementImplStub<KotlinContractEffectStub> {
    constructor(node: ASTNode): super(node)
    constructor(stub: KotlinContractEffectStub): super(stub, KtStubBasedElementTypes.CONTRACT_EFFECT)
}

fun KtContractEffect.getExpression(): KtExpression = getChildOfType()!!
