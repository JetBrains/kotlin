/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi

import com.intellij.lang.ASTNode
import org.jetbrains.kotlin.KtStubBasedElementTypes
import org.jetbrains.kotlin.psi.stubs.KotlinPlaceHolderStub

/**
 * The class representing a context parameter list in a declaration or functional type.
 *
 * ### Example
 * ```kotlin
 * class Context
 *
 * context(c: Context) // named context parameter
 * fun withNamedContext() {}
 *
 * context(_: Context) // unnamed context parameter
 * fun withUnnamedContext() {}
 *
 * val myAction: context(Context) () -> Unit = {} // context parameter on a functional type doesn't have names
 * ```
 *
 * [KEEP-367](https://github.com/Kotlin/KEEP/blob/main/proposals/KEEP-0367-context-parameters.md)
 */
@SubclassOptInRequired(KtImplementationDetail::class)
abstract class KtContextParameterList : KtElementImplStub<KotlinPlaceHolderStub<KtContextParameterList>> {
    constructor(node: ASTNode) : super(node)
    constructor(stub: KotlinPlaceHolderStub<KtContextParameterList>) : super(stub, KtStubBasedElementTypes.CONTEXT_PARAMETER_LIST)

    override fun <R, D> accept(visitor: KtVisitor<R, D>, data: D): R {
        return visitor.visitContextParameterList(this, data)
    }

    /**
     * Returns the context parameters within the list.
     *
     * Note that [KtFunctionType] still uses [contextReceivers] for compatibility with K1.
     */
    @Deprecated("Use 'contextParameters' instead", ReplaceWith("contextParameters"))
    fun contextParameters(): List<KtParameter> = contextParameters

    /**
     * Returns the context parameters within the list.
     *
     * Note that [KtFunctionType] still uses [contextReceivers] for compatibility with K1.
     */
    val contextParameters: List<KtParameter>
        get() = getStubOrPsiChildrenAsList(KtStubBasedElementTypes.VALUE_PARAMETER)

    /**
     * Returns the context receiver within the list.
     *
     * This is API is obsolete for declarations ([contextParameters] has to be used instead),
     * but it is still used for [KtFunctionType] for compatibility with K1.
     *
     * This API will be removed in the future (together with [KtContextReceiver]).
     *
     * @see contextParameters
     */
    fun contextReceivers(): List<KtContextReceiver> = getStubOrPsiChildrenAsList(KtStubBasedElementTypes.CONTEXT_RECEIVER)

    /**
     * Return the list of type references from context receivers.
     *
     * This is API is obsolete for declarations ([contextParameters] has to be used instead),
     * but it is still used for [KtFunctionType] for compatibility with K1.
     *
     * This API will be removed in the future (together with [KtContextReceiver]).
     *
     * @see contextReceivers
     */
    fun typeReferences(): List<KtTypeReference> = contextReceivers().mapNotNull { it.typeReference() }

    /**
     * Returns the owner declaration of the context parameter list.
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
