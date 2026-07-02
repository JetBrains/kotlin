/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
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
 * Represents a single context receiver in a context receiver list.
 *
 * ### Example:
 *
 * ```kotlin
 * context(Logger)
 * //      ^^^^^^
 * fun log(message: String) {}
 * ```
 *
 * Deprecated in favor of context parameters.
 */
class KtContextReceiver : KtElementImplStub<KotlinContextReceiverStub> {
    constructor(node: ASTNode) : super(node)
    constructor(stub: KotlinContextReceiverStub) : super(stub, KtStubBasedElementTypes.CONTEXT_RECEIVER)

    override fun <R : Any?, D : Any?> accept(visitor: KtVisitor<R, D>, data: D): R {
        return visitor.visitContextReceiver(this, data)
    }

    /**
     * Returns the explicit label of this context receiver (as in `context(logger@ Logger)`), or `null` if it has no
     * label.
     */
    fun targetLabel(): KtSimpleNameExpression? =
        findChildByType<KtContainerNode?>(KtNodeTypes.LABEL_QUALIFIER)
            ?.findChildByType(KtNodeTypes.LABEL)

    /**
     * Returns the explicit label name, or `null` if this context receiver has no label.
     */
    fun labelName(): String? {
        stub?.let { return it.label }
        return targetLabel()?.getReferencedName()
    }

    /**
     * Returns the explicit label name as a [Name], or `null` if this context receiver has no label.
     */
    fun labelNameAsName(): Name? {
        stub?.let { stub -> return stub.label?.let { Name.identifier(it) } }
        return targetLabel()?.getReferencedNameAsName()
    }

    /**
     * Returns the type reference of this context receiver, or `null` if it is absent in incomplete code.
     */
    @Suppress("DEPRECATION") // KT-78356
    fun typeReference(): KtTypeReference? = getStubOrPsiChild(KtStubBasedElementTypes.TYPE_REFERENCE)

    /**
     * Returns the effective name used to reference this context receiver: the explicit [labelName] if present,
     * otherwise the receiver type's short name, or `null` if neither is available.
     */
    fun name(): String? = labelName() ?: typeReference()?.nameForReceiverLabel()

    /**
     * Returns the owner declaration of the context receiver.
     * The owner would be null in the case of a context parameter on a functional type.
     *
     * @see KtContextParameterList.ownerDeclaration
     */
    val ownerDeclaration: KtDeclaration?
        get() {
            val contextReceiverList = parent
            requireWithAttachment(
                contextReceiverList is KtContextParameterList,
                { "parent should be ${KtContextParameterList::class.simpleName}" },
            ) {
                withPsiEntry("psi", this@KtContextReceiver)
                withPsiEntry("parent", parent)
            }

            return contextReceiverList.ownerDeclaration
        }
}