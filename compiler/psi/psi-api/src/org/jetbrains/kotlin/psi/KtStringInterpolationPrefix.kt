/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.KtStubBasedElementTypes
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.stubs.KotlinStringInterpolationPrefixStub

/**
 * Represents the multi-dollar interpolation prefix for string templates.
 *
 * ### Example:
 *
 * ```kotlin
 * val s = $$"Price: $$price"
 * //      ^^
 *
 * val name = "Kotlin"
 *
 * "I'm $name" // I'm Kotlin
 * $"I'm $name" // I'm Kotlin
 *
 * $$"I'm $name" // I'm $name
 * $$"I'm $$name" // I'm Kotlin
 * ```
 */
class KtStringInterpolationPrefix : KtElementImplStub<KotlinStringInterpolationPrefixStub> {
    constructor(node: ASTNode) : super(node)
    constructor(stub: KotlinStringInterpolationPrefixStub) : super(stub, KtStubBasedElementTypes.STRING_INTERPOLATION_PREFIX)

    /**
     * The interpolation prefix.
     * Consists of repeated `$` characters.
     *
     * #### Example
     *
     * ```kotlin
     * $"" // $
     * $$$"" // $$$
     * ```
     */
    val interpolationPrefix: String
        get() {
            val stub = greenStub
            return if (stub != null) {
                "$".repeat(stub.dollarSignCount)
            } else {
                interpolationPrefixElement?.text ?: ""
            }
        }

    override fun getText(): String = interpolationPrefix

    /**
     * The [PsiElement] which represent [KtTokens.INTERPOLATION_PREFIX].
     */
    val interpolationPrefixElement: PsiElement?
        @IfNotParsed
        get() = findChildByType(KtTokens.INTERPOLATION_PREFIX)

    override fun <R, D> accept(visitor: KtVisitor<R, D>, data: D): R {
        return visitor.visitStringInterpolationPrefix(this, data);
    }
}
