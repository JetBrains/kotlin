/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi

import com.intellij.lang.ASTNode
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType
import org.jetbrains.kotlin.psi.stubs.KotlinPlaceHolderStub

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
class KtContractEffect : KtElementImplStub<KotlinPlaceHolderStub<KtContractEffect>> {
    @KtImplementationDetail
    constructor(node: ASTNode) : super(node)

    @KtImplementationDetail
    constructor(stub: KotlinPlaceHolderStub<KtContractEffect>) : super(stub, KtNodeTypes.CONTRACT_EFFECT)

    companion object {
        /** A shared empty array, which can be reused to avoid unnecessary allocations. */
        @JvmField
        val EMPTY_ARRAY: Array<KtContractEffect> = emptyArray()
    }
}

/**
 * Returns the expression that describes this contract effect (for example, `returns() implies (s != null)`).
 */
fun KtContractEffect.getExpression(): KtExpression = getChildOfType()!!
