/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi

/**
 * Represents a companion block.
 *
 * ### Example:
 *
 * ```kotlin
 * class C {
 *     companion {
 *        fun foo() {}
 *     }
 * //  ^__________^
 * //  The entire companion block
 * }
 * ```
 *
 * [KEEP-0449](https://github.com/Kotlin/KEEP/blob/main/proposals/KEEP-0449-companions-block-extension.md)
 */
@KtExperimentalApi
@SubclassOptInRequired(KtImplementationDetail::class)
interface KtCompanionBlock : KtElement, KtDeclarationContainer {
    /**
     * The body of the companion block.
     */
    val body: KtClassBody

    /**
     * The list of declarations inside the companion block.
     */
    override fun getDeclarations(): List<KtDeclaration>

    @KtExperimentalApi
    companion object {
        /** A shared empty array, which can be reused to avoid unnecessary allocations. */
        @JvmField
        val EMPTY_ARRAY: Array<KtCompanionBlock> = emptyArray()
    }
}
