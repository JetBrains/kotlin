/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi

import com.intellij.lang.ASTNode
import org.jetbrains.kotlin.KtStubBasedElementTypes
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType
import org.jetbrains.kotlin.psi.stubs.KotlinContractEffectStub

/**
 * Represents a single effect declaration inside a contract block.
 *
 * ### Example:
 *
 * ```kotlin
 * fun foo(s: String?) contract [returns() implies (s != null)] {
 * //                            ^___________________________^
 *     println("Hello")
 * }
 * ```
 */
@OptIn(KtImplementationDetail::class)
class KtContractEffect : KtElementImplStub<KotlinContractEffectStub> {
    @KtImplementationDetail
    constructor(node: ASTNode) : super(node)

    @KtImplementationDetail
    constructor(stub: KotlinContractEffectStub) : super(stub, KtStubBasedElementTypes.CONTRACT_EFFECT)
}

/**
 * Returns the expression that describes this contract effect (for example, `returns() implies (s != null)`).
 */
fun KtContractEffect.getExpression(): KtExpression = getChildOfType()!!
