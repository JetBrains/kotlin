/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi

import com.intellij.lang.ASTNode
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.KtStubBasedElementTypes
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.stubs.KotlinContextReceiverStub
import org.jetbrains.kotlin.utils.exceptions.requireWithAttachment
import org.jetbrains.kotlin.utils.exceptions.withPsiEntry

/**
 * Deprecated in favor of context parameters.
 */
class KtContextReceiver : KtElementImplStub<KotlinContextReceiverStub> {
    constructor(node: ASTNode) : super(node)
    constructor(stub: KotlinContextReceiverStub) : super(stub, KtStubBasedElementTypes.CONTEXT_RECEIVER)

    override fun <R : Any?, D : Any?> accept(visitor: KtVisitor<R, D>, data: D): R {
        return visitor.visitContextReceiver(this, data)
    }

    fun targetLabel(): KtSimpleNameExpression? =
        findChildByType<KtContainerNode?>(KtNodeTypes.LABEL_QUALIFIER)
            ?.findChildByType(KtNodeTypes.LABEL)

    fun labelName(): String? {
        stub?.let { return it.label }
        return targetLabel()?.getReferencedName()
    }

    fun labelNameAsName(): Name? {
        stub?.let { stub -> return stub.label?.let { Name.identifier(it) } }
        return targetLabel()?.getReferencedNameAsName()
    }

    fun typeReference(): KtTypeReference? =
        @Suppress("DEPRECATION")
        getStubOrPsiChild(KtStubBasedElementTypes.TYPE_REFERENCE)

    fun name(): String? = labelName() ?: typeReference()?.nameForReceiverLabel()

    /**
     * Returns the owner declaration of the context receiver.
     * The owner would be null in the case of context parameter on a functional type.
     *
     * @see KtContextReceiverList.ownerDeclaration
     */
    val ownerDeclaration: KtDeclaration?
        get() {
            val contextReceiverList = parent
            requireWithAttachment(
                contextReceiverList is KtContextReceiverList,
                { "parent should be ${KtContextReceiverList::class.simpleName}" },
            ) {
                withPsiEntry("psi", this@KtContextReceiver)
                withPsiEntry("parent", parent)
            }

            return contextReceiverList.ownerDeclaration
        }
}