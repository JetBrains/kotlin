/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi

import com.intellij.lang.ASTNode
import org.jetbrains.kotlin.psi.stubs.KotlinPlaceHolderStub
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes

class KtContextReceiverList : KtElementImplStub<KotlinPlaceHolderStub<KtContextReceiverList>> {
    constructor(node: ASTNode) : super(node)
    constructor(stub: KotlinPlaceHolderStub<KtContextReceiverList>) : super(stub, KtStubElementTypes.CONTEXT_RECEIVER_LIST)

    override fun <R : Any?, D : Any?> accept(visitor: KtVisitor<R, D>, data: D): R {
        return visitor.visitContextReceiverList(this, data)
    }

    fun contextReceivers(): List<KtContextReceiver> = getStubOrPsiChildrenAsList(KtStubElementTypes.CONTEXT_RECEIVER)

    fun contextParameters(): List<KtParameter> = getStubOrPsiChildrenAsList(KtStubElementTypes.VALUE_PARAMETER)

    fun typeReferences(): List<KtTypeReference> = contextReceivers().mapNotNull { it.typeReference() }

    /**
     * Returns the owner declaration of the context receiver list.
     * The owner would be null in the case of context parameter on a functional type.
     * ```kotlin
     * fun foo(action: context(Int) () -> Unit
     * ```
     *
     * In this case the owner for `context(Int)` will be **null**.
     */
    val ownerDeclaration: KtDeclaration?
        get() {
            val modifierList = parent as? KtModifierList ?: return null
            return modifierList.owner as? KtDeclaration
        }
}
