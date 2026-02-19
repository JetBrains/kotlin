/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi

import com.intellij.lang.ASTNode
import org.jetbrains.kotlin.KtStubBasedElementTypes
import org.jetbrains.kotlin.psi.stubs.KotlinPlaceHolderStub

/**
 * Represents the list of contract effects in a contract block.
 *
 * ### Example:
 *
 * ```kotlin
 * fun baz(num: Int?, block: () -> Unit): Int contract [
 *        callsInPlace(block, InvocationKind.EXACTLY_ONCE),
 *        returns() implies (num != null)
 *    ] {
 * // ^________________________________________________^
 * // The entire block from '[' to ']'
 *     println("Hello")
 * }
 * ```
 */
class KtContractEffectList : KtElementImplStub<KotlinPlaceHolderStub<KtContractEffectList>> {
    constructor(node: ASTNode) : super(node)
    constructor(stub: KotlinPlaceHolderStub<KtContractEffectList>) : super(stub, KtStubBasedElementTypes.CONTRACT_EFFECT_LIST)
}

fun KtContractEffectList.getContractEffects(): List<KtContractEffect> =
    getStubOrPsiChildrenAsList(KtStubBasedElementTypes.CONTRACT_EFFECT)
