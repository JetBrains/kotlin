/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi

import com.intellij.lang.ASTNode
import org.jetbrains.kotlin.psi.stubs.KotlinPlaceHolderStub
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes

class KtRefinementType : KtElementImplStub<KotlinPlaceHolderStub<KtIntersectionType>>, KtTypeElement {
    constructor(node: ASTNode) : super(node)
    constructor(stub: KotlinPlaceHolderStub<KtIntersectionType>) : super(stub, KtStubElementTypes.INTERSECTION_TYPE)

    override fun getTypeArgumentsAsTypes(): List<KtTypeReference?> = emptyList()
}