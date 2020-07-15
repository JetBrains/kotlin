/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi

import com.intellij.lang.ASTNode
import org.jetbrains.kotlin.psi.stubs.KotlinContractEffectStub
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes

class KtContractEffect: KtElementImplStub<KotlinContractEffectStub> { // TODO: check constructors
    public constructor(node: ASTNode): super(node)
    public constructor(stub: KotlinContractEffectStub): super(stub, KtStubElementTypes.CONTRACT_EFFECT)
}