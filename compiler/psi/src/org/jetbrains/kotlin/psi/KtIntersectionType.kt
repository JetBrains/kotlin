/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi

import com.intellij.lang.ASTNode
import org.jetbrains.kotlin.psi.stubs.KotlinPlaceHolderStub
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes

class KtIntersectionType : KtElementImplStub<KotlinPlaceHolderStub<KtIntersectionType>>, KtTypeElement {
    constructor(node: ASTNode) : super(node)
    constructor(stub: KotlinPlaceHolderStub<KtIntersectionType>) : super(stub, KtStubElementTypes.INTERSECTION_TYPE)

    override fun getTypeArgumentsAsTypes(): List<KtTypeReference> = emptyList()

    fun getLeftTypeRef(): KtTypeReference? = getStubOrPsiChildrenAsList(KtStubElementTypes.TYPE_REFERENCE).getOrNull(0)
    fun getRightTypeRef(): KtTypeReference? = getStubOrPsiChildrenAsList(KtStubElementTypes.TYPE_REFERENCE).getOrNull(1)

    override fun <R, D> accept(visitor: KtVisitor<R, D>, data: D): R {
        return visitor.visitIntersectionType(this, data)
    }
}
