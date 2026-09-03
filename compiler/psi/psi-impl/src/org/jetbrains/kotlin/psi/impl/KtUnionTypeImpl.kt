/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.impl

import com.intellij.lang.ASTNode
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.psi.KtElementImplStub
import org.jetbrains.kotlin.psi.KtExperimentalApi
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.psi.KtUnionType
import org.jetbrains.kotlin.psi.KtVisitor
import org.jetbrains.kotlin.psi.stubs.KotlinPlaceHolderStub

@OptIn(KtImplementationDetail::class, KtExperimentalApi::class)
internal class KtUnionTypeImpl : KtElementImplStub<KotlinPlaceHolderStub<KtUnionTypeImpl>>, KtUnionType {
    constructor(node: ASTNode) : super(node)
    constructor(stub: KotlinPlaceHolderStub<KtUnionTypeImpl>) : super(stub, KtNodeTypes.UNION_TYPE)

    override val types: List<KtTypeReference>
        get() = getStubOrPsiChildren(KtNodeTypes.TYPE_REFERENCE, KtTypeReference.EMPTY_ARRAY).asList()

    override fun <R, D> accept(visitor: KtVisitor<R, D>, data: D): R {
        return visitor.visitUnionType(this, data)
    }
}
