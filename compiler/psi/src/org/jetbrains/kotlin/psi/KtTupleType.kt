/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi

import com.intellij.lang.ASTNode
import org.jetbrains.kotlin.psi.stubs.KotlinPlaceHolderStub
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes

class KtTupleType : KtElementImplStub<KotlinPlaceHolderStub<KtTupleType>>, KtTypeElement {
    constructor(node: ASTNode) : super(node)
    constructor(stub: KotlinPlaceHolderStub<KtTupleType>) : super(stub, KtStubElementTypes.TUPLE_TYPE);

    val innerType: KtTypeElement?
        get() = KtStubbedPsiUtil.getStubOrPsiChild(this, KtStubElementTypes.TYPE_ELEMENT_TYPES, KtTypeElement.ARRAY_FACTORY)

    override fun <R : Any?, D : Any?> accept(visitor: KtVisitor<R, D>, data: D): R {
        return visitor.visitTupleType(this, data)
    }

    override fun getTypeArgumentsAsTypes(): MutableList<KtTypeReference> {
        return innerType?.typeArgumentsAsTypes ?: mutableListOf()
    }
}