/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi

import com.intellij.lang.ASTNode
import org.jetbrains.kotlin.psi.stubs.KotlinContractExpressionStub
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes

class KtContractExpression: KtElementImplStub<KotlinContractExpressionStub> {
    constructor(node: ASTNode): super(node)
    constructor(stub: KotlinContractExpressionStub): super(stub, KtStubElementTypes.CONTRACT_EXPRESSION)
}