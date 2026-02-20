/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi

import com.intellij.lang.ASTNode
import org.jetbrains.kotlin.KtStubBasedElementTypes
import org.jetbrains.kotlin.psi.stubs.KotlinPlaceHolderStub

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
class KtCompanionBlock : KtElementImplStub<KotlinPlaceHolderStub<KtCompanionBlock>>, KtDeclarationContainer {
    constructor(stub: KotlinPlaceHolderStub<KtCompanionBlock>) : super(stub, KtStubBasedElementTypes.COMPANION_BLOCK)
    constructor(node: ASTNode) : super(node)

    override fun <R, D> accept(visitor: KtVisitor<R, D>, data: D): R {
        return visitor.visitCompanionBlock(this, data)
    }

    /**
     * The body of the companion block.
     */
    val body: KtClassBody
        get() {
            @Suppress("DEPRECATION") // KT-78356
            return getStubOrPsiChild(KtStubBasedElementTypes.CLASS_BODY)!!
        }

    /**
     * The list of declarations inside the companion block.
     */
    override fun getDeclarations(): List<KtDeclaration> {
        return body.declarations
    }
}
