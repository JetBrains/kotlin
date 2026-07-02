/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(KtNonPublicApi::class)

package org.jetbrains.kotlin.psi

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.KtStubBasedElementTypes
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.psi.stubs.KotlinConstructorStub

/**
 * Represents a secondary constructor declared in the class body.
 *
 * ### Example:
 *
 * ```kotlin
 * class Person(val name: String, val age: Int) {
 *     constructor(name: String) : this(name, 0)
 * //  ^_______________________________________^
 * }
 * ```
 */
class KtSecondaryConstructor : KtConstructor<KtSecondaryConstructor> {
    constructor(node: ASTNode) : super(node)
    constructor(stub: KotlinConstructorStub<KtSecondaryConstructor>) : super(stub, KtStubBasedElementTypes.SECONDARY_CONSTRUCTOR)

    override fun <R, D> accept(visitor: KtVisitor<R, D>, data: D) = visitor.visitSecondaryConstructor(this, data)

    override fun getContainingClassOrObject(): KtClassOrObject = containingClassOrObject!!

    override fun getBodyExpression(): KtBlockExpression? {
        greenStub?.let {
            if (!it.hasBody) return null
        }

        return findChildByClass(KtBlockExpression::class.java)
    }

    override fun getConstructorKeyword() = notNullChild<PsiElement>(super.getConstructorKeyword())

    /**
     * Returns the delegation call to another constructor (`: this(...)` / `: super(...)`). A delegation call is always
     * present in the tree, even when implicit (see [KtConstructorDelegationCall.isImplicit]); use [getDelegationCallOrNull]
     * to tolerate incomplete code.
     */
    fun getDelegationCall(): KtConstructorDelegationCall = findNotNullChildByClass(KtConstructorDelegationCall::class.java)

    /**
     * Returns the delegation call, or `null` if it is absent in incomplete code.
     */
    fun getDelegationCallOrNull(): KtConstructorDelegationCall? = findChildByClass(KtConstructorDelegationCall::class.java)

    /**
     * Returns `true` if the delegation call is implicit, that is, not written in the source.
     */
    fun hasImplicitDelegationCall(): Boolean = getDelegationCall().isImplicit

    @Deprecated(
        "Use convertImplicitDelegationCallToExplicit(isThis) instead",
        ReplaceWith(
            "this.convertImplicitDelegationCallToExplicit(isThis)",
            "org.jetbrains.kotlin.idea.base.psi.convertImplicitDelegationCallToExplicit",
        ),
    )
    fun replaceImplicitDelegationCallWithExplicit(isThis: Boolean): KtConstructorDelegationCall =
        KtPsiMutationService.getInstance().convertImplicitDelegationCallToExplicit(this, isThis)
}
