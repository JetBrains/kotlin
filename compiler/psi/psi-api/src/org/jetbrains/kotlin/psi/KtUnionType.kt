/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi

import com.intellij.lang.ASTNode
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.psi.stubs.KotlinPlaceHolderStub

/**
 * Represents an union type combining two types with {@code |}.
 *
 * ### Example:
 *
 * ```kotlin
 * fun foo(): Int | MyError {
 * //         ^___________^
 * }
 * ```
 */
@KtExperimentalApi
@OptIn(KtImplementationDetail::class)
class KtUnionType : KtElementImplStub<KotlinPlaceHolderStub<KtUnionType>>, KtTypeElement {
    @KtImplementationDetail
    constructor(node: ASTNode) : super(node)

    @KtImplementationDetail
    constructor(stub: KotlinPlaceHolderStub<KtUnionType>) : super(stub, KtNodeTypes.UNION_TYPE)

    /** Always empty: a union type has no type arguments. */
    override fun getTypeArgumentsAsTypes(): List<KtTypeReference> = emptyList()

    val types: Array<out KtTypeReference>
        get() = getStubOrPsiChildren(KtNodeTypes.TYPE_REFERENCE, KtTypeReference.EMPTY_ARRAY)

    override fun <R, D> accept(visitor: KtVisitor<R, D>, data: D): R {
        return visitor.visitUnionType(this, data)
    }
}
