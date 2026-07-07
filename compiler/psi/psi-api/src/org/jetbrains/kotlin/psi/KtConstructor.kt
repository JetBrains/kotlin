/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi

import com.intellij.lang.ASTNode
import com.intellij.navigation.ItemPresentationProviders
import com.intellij.psi.PsiElement
import com.intellij.psi.search.SearchScope
import com.intellij.util.IncorrectOperationException
import org.jetbrains.kotlin.KtStubBasedElementTypes
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.psiUtil.isLegacyContractPresentPsiCheck
import org.jetbrains.kotlin.psi.stubs.KotlinConstructorStub
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementType

/**
 * Represents a constructor of a class or object.
 *
 * This is the common base for the concrete node types [KtPrimaryConstructor] and [KtSecondaryConstructor]. A
 * constructor is a [KtFunction] with value parameters and (for secondary constructors) a body, but it has no name,
 * receiver, return type, or type parameters of its own.
 *
 * ### Example:
 *
 * ```kotlin
 * class Foo(val x: Int) {
 * //       ^__________^
 * //       The primary constructor
 *     constructor() : this(0)
 * //  ^_____________________^
 * //  A secondary constructor
 * }
 * ```
 *
 * @param T the concrete constructor node type, used by the stub machinery
 */
abstract class KtConstructor<T : KtConstructor<T>> : KtDeclarationStub<KotlinConstructorStub<T>>, KtFunction {
    protected constructor(node: ASTNode) : super(node)
    protected constructor(
        stub: KotlinConstructorStub<T>,
        nodeType: KtStubElementType<out KotlinConstructorStub<T>, T>,
    ) : super(stub, nodeType)

    /**
     * Returns the class or object that this constructor belongs to.
     */
    abstract fun getContainingClassOrObject(): KtClassOrObject

    /** Always `false`: a constructor is never a local declaration. */
    override fun isLocal() = false

    override fun getValueParameterList() =
        @Suppress("DEPRECATION") // KT-78356
        getStubOrPsiChild(KtStubBasedElementTypes.VALUE_PARAMETER_LIST)

    override fun getValueParameters() = valueParameterList?.parameters ?: emptyList()

    /** Always `null`: a constructor cannot be an extension, so it has no receiver type. */
    override fun getReceiverTypeReference() = null

    /** Always `null`: a constructor has no return-type reference. */
    override fun getTypeReference() = null

    @Suppress("OVERRIDE_DEPRECATION")
    @Throws(IncorrectOperationException::class)
    override fun setTypeReference(typeRef: KtTypeReference?) = throw IncorrectOperationException("setTypeReference to constructor")

    override fun getColon() = findChildByType<PsiElement>(KtTokens.COLON)

    /**
     * A constructor's body is always a [KtBlockExpression] (a constructor cannot have an expression body); `null` if it
     * has no body. The base implementation returns `null`; [KtSecondaryConstructor] overrides it.
     */
    override fun getBodyExpression(): KtBlockExpression? = null

    /** Always `null`: a constructor cannot have an expression body, so there is no `=` token. */
    override fun getEqualsToken() = null

    override fun hasBlockBody() = hasBody()

    /**
     * Returns `true` if this constructor delegates to another constructor of the same class (`: this(...)`), rather
     * than to a superclass constructor. For a primary constructor this is always `false`.
     */
    fun isDelegatedCallToThis(): Boolean {
        greenStub?.let { return it.isDelegatedCallToThis }
        return when (this) {
            is KtPrimaryConstructor -> false
            is KtSecondaryConstructor -> getDelegationCallOrNull()?.isCallToThis() ?: true
            else -> throw IllegalStateException("Unknown constructor type: $this")
        }
    }

    /**
     * Returns `true` if this constructor has an explicit delegation call written in the source (`: this(...)` or
     * `: super(...)`). For a primary constructor this is always `false`.
     */
    fun isExplicitDelegationCall(): Boolean {
        greenStub?.let { return it.isExplicitDelegationCall }
        return when (this) {
            is KtPrimaryConstructor -> false
            is KtSecondaryConstructor -> getDelegationCallOrNull()?.isImplicit == false
            else -> throw IllegalStateException("Unknown constructor type: $this")
        }
    }

    override fun hasBody(): Boolean {
        greenStub?.let { return it.hasBody }
        return bodyExpression != null
    }

    /** Always `false`: a constructor never declares a return type. */
    override fun hasDeclaredReturnType() = false

    /** Always `null`: a constructor cannot declare type parameters. */
    override fun getTypeParameterList() = null

    /** Always `null`: a constructor cannot have a `where` clause. */
    override fun getTypeConstraintList() = null

    /** Always empty: a constructor has no type constraints. */
    override fun getTypeConstraints() = emptyList<KtTypeConstraint>()

    /** Always empty: a constructor cannot declare type parameters. */
    override fun getTypeParameters() = emptyList<KtTypeParameter>()

    /** A constructor has no name of its own; returns the name of its [containing class][getContainingClassOrObject]. */
    override fun getName(): String? = getContainingClassOrObject().name

    override fun getNameAsSafeName() = KtPsiUtil.safeName(name)

    /** Always `null`: a constructor has no fully qualified name of its own. */
    override fun getFqName() = null

    override fun getNameAsName() = nameAsSafeName

    /** Always `null`: a constructor has no name identifier. */
    override fun getNameIdentifier() = null

    override fun getIdentifyingElement(): PsiElement? = getConstructorKeyword()

    @Throws(IncorrectOperationException::class)
    override fun setName(name: String): PsiElement = throw IncorrectOperationException("setName to constructor")

    override fun getPresentation() = ItemPresentationProviders.getItemPresentation(this)

    /**
     * Returns the `constructor` keyword, or `null` if it is omitted (for a primary constructor without modifiers or
     * annotations the keyword is optional).
     */
    open fun getConstructorKeyword(): PsiElement? = findChildByType(KtTokens.CONSTRUCTOR_KEYWORD)

    /**
     * Returns `true` if this constructor has the `constructor` keyword.
     */
    fun hasConstructorKeyword(): Boolean = stub != null || getConstructorKeyword() != null

    override fun mayHaveContract(): Boolean {
        val stub = greenStub
        if (stub != null) {
            return stub.mayHaveContract
        }

        @OptIn(KtImplementationDetail::class)
        return isLegacyContractPresentPsiCheck()
    }

    override fun getTextOffset(): Int {
        return getConstructorKeyword()?.textOffset
            ?: valueParameterList?.textOffset
            ?: super.getTextOffset()
    }

    override fun getUseScope(): SearchScope {
        return getContainingClassOrObject().useScope
    }
}
