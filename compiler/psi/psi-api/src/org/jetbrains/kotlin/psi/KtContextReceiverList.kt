/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi

import com.intellij.lang.ASTNode
import org.jetbrains.kotlin.KtStubBasedElementTypes
import org.jetbrains.kotlin.psi.stubs.KotlinPlaceHolderStub

class KtContextReceiverList : KtElementImplStub<KotlinPlaceHolderStub<KtContextReceiverList>> {
    constructor(node: ASTNode) : super(node)
    constructor(stub: KotlinPlaceHolderStub<KtContextReceiverList>) : super(stub, KtStubBasedElementTypes.CONTEXT_PARAMETER_LIST)

    override fun <R : Any?, D : Any?> accept(visitor: KtVisitor<R, D>, data: D): R {
        return visitor.visitContextReceiverList(this, data)
    }

    fun contextReceivers(): List<KtContextReceiver> = getStubOrPsiChildrenAsList(KtStubBasedElementTypes.CONTEXT_RECEIVER)

    fun contextParameters(): List<KtParameter> = getStubOrPsiChildrenAsList(KtStubBasedElementTypes.VALUE_PARAMETER)

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
