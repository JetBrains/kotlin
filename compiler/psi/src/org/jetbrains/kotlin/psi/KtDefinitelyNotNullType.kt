/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi

import com.intellij.lang.ASTNode
import org.jetbrains.kotlin.psi.stubs.KotlinPlaceHolderStub
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes

class KtDefinitelyNotNullType : KtElementImplStub<KotlinPlaceHolderStub<KtDefinitelyNotNullType>>, KtTypeElement {
    constructor(node: ASTNode) : super(node)
    constructor(stub: KotlinPlaceHolderStub<KtDefinitelyNotNullType>) : super(stub, KtStubElementTypes.DEFINITELY_NOT_NULL_TYPE)

    override fun getTypeArgumentsAsTypes(): List<KtTypeReference> {
        return this.innerType?.typeArgumentsAsTypes ?: emptyList()
    }

    override fun <R, D> accept(visitor: KtVisitor<R, D>, data: D): R {
        return visitor.visitDefinitelyNotNullType(this, data)
    }

    @get:IfNotParsed
    val innerType: KtTypeElement?
        get() = KtStubbedPsiUtil.getStubOrPsiChild(this, KtStubElementTypes.TYPE_ELEMENT_TYPES, KtTypeElement.ARRAY_FACTORY)

    val modifierList: KtModifierList?
        get() = getStubOrPsiChild(KtStubElementTypes.MODIFIER_LIST)

    val annotationEntries: List<KtAnnotationEntry>
        get() {
            return modifierList?.annotationEntries ?: emptyList()
        }
}
