/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi

import com.intellij.lang.ASTNode
import org.jetbrains.kotlin.KtStubBasedElementTypes
import org.jetbrains.kotlin.psi.stubs.KotlinPlaceHolderStub

/**
 * Represents an intersection type combining two types with {@code &}.
 *
 * ### Example:
 *
 * ```kotlin
 * fun <T> foo(x: T & Any) {
 * //             ^_____^
 * }
 * ```
 */
class KtIntersectionType : KtElementImplStub<KotlinPlaceHolderStub<KtIntersectionType>>, KtTypeElement {
    constructor(node: ASTNode) : super(node)
    constructor(stub: KotlinPlaceHolderStub<KtIntersectionType>) : super(stub, KtStubBasedElementTypes.INTERSECTION_TYPE)

    /** Always empty: an intersection type has no type arguments (its operands are [getLeftTypeRef] and [getRightTypeRef]). */
    override fun getTypeArgumentsAsTypes(): List<KtTypeReference> = emptyList()

    /**
     * Returns the left operand type of the intersection (the part before `&`), or `null` if it is absent in incomplete
     * code.
     */
    fun getLeftTypeRef(): KtTypeReference? = getStubOrPsiChildrenAsList(KtStubBasedElementTypes.TYPE_REFERENCE).getOrNull(0)

    /**
     * Returns the right operand type of the intersection (the part after `&`), or `null` if it is absent in incomplete
     * code.
     */
    fun getRightTypeRef(): KtTypeReference? = getStubOrPsiChildrenAsList(KtStubBasedElementTypes.TYPE_REFERENCE).getOrNull(1)

    override fun <R, D> accept(visitor: KtVisitor<R, D>, data: D): R {
        return visitor.visitIntersectionType(this, data)
    }
}
