/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi

import com.intellij.lang.ASTNode
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.psi.stubs.KotlinPlaceHolderStub

/**
 * The class representing a context parameter list in a declaration or function type.
 *
 * ### Example
 * ```kotlin
 * class Context
 *
 * context(c: Context) // named context parameter
 * fun withNamedContext() {}
 *
 * context(_: Context) // anonymous context parameter
 * fun withAnonymousContext() {}
 *
 * val myAction: context(Context) () -> Unit = {} // context parameter on a function type doesn't have a name
 *
 * context(Context) // error: context parameter without a name (the syntax of legacy context receivers)
 * fun withLegacyContextReceiver() {}
 * ```
 *
 * [KEEP-367](https://github.com/Kotlin/KEEP/blob/main/proposals/KEEP-0367-context-parameters.md)
 */
@SubclassOptInRequired(KtImplementationDetail::class)
abstract class KtContextParameterList : KtElementImplStub<KotlinPlaceHolderStub<KtContextParameterList>> {
    @KtImplementationDetail
    constructor(node: ASTNode) : super(node)

    @KtImplementationDetail
    constructor(stub: KotlinPlaceHolderStub<KtContextParameterList>) : super(stub, KtNodeTypes.CONTEXT_PARAMETER_LIST)

    override fun <R, D> accept(visitor: KtVisitor<R, D>, data: D): R {
        return visitor.visitContextParameterList(this, data)
    }

    /**
     * Returns the context parameters within the list.
     */
    @Deprecated(
        message = "Use 'contextParameters' instead",
        replaceWith = ReplaceWith("contextParameters"),
        level = DeprecationLevel.HIDDEN,
    )
    fun contextParameters(): List<KtParameter> = contextParameters

    /**
     * Returns the context parameters within the list.
     */
    val contextParameters: List<KtParameter>
        get() = getStubOrPsiChildren(KtNodeTypes.VALUE_PARAMETER, KtParameter.EMPTY_ARRAY).asList()

    /**
     * Returns an empty list: context receivers are parsed as context parameters without names, which [contextParameters] returns.
     *
     * This API will be removed in the future (together with [KtContextReceiver]).
     *
     * @see contextParameters
     */
    @Deprecated(
        message = "Use 'contextParameters' instead. Context receivers are parsed as context parameters without names.",
        level = DeprecationLevel.WARNING,
    )
    @Suppress("DEPRECATION")
    fun contextReceivers(): List<KtContextReceiver> = emptyList()

    /**
     * Returns the type references of the [contextParameters].
     *
     * @see contextParameters
     */
    @Deprecated(
        message = "Use 'contextParameters' instead.",
        replaceWith = ReplaceWith("contextParameters.mapNotNull { it.typeReference }"),
        level = DeprecationLevel.WARNING,
    )
    fun typeReferences(): List<KtTypeReference> = contextParameters.mapNotNull { it.typeReference }

    /**
     * Returns the owner declaration of the context parameter list.
     * The owner would be null in the case of context parameter on a function type.
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

    companion object {
        /** A shared empty array, which can be reused to avoid unnecessary allocations. */
        @JvmField
        val EMPTY_ARRAY: Array<KtContextParameterList> = emptyArray()
    }
}
