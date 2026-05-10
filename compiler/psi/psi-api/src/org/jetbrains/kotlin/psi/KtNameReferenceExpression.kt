/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import org.jetbrains.kotlin.KtStubBasedElementTypes
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.lexer.KtTokens.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.stubs.KotlinNameReferenceExpressionStub
import org.jetbrains.kotlin.resolution.KtResolvableCall

/**
 * Represents a simple name reference to a variable, function, or type.
 *
 * ### Example:
 *
 * ```kotlin
 * val x = foo
 * //      ^_^
 * ```
 *
 * ### Analysis API Resolver Notes:
 *
 * Resolves the declaration symbol referenced by the given [KtNameReferenceExpression].
 *
 * **Note:** Unlike other `KtResolvableCall` entry points that provide both `resolveCall`
 * and `resolveSymbol` specializations, `KtNameReferenceExpression.resolveCall` may return a different `KaSymbol`.
 *
 * For instance, this happens for constructor references. While `resolveCall` returns a
 * `KaConstructorSymbol`, this method returns the corresponding `KaClassLikeSymbol`.
 *
 * #### Example #1
 *
 * ```kotlin
 * fun foo() {}
 *
 * val x = foo
 * //      ^^^
 * ```
 *
 * Calling `resolveSymbol()` on the `KtNameReferenceExpression` (`foo`) returns the `KaDeclarationSymbol` of `foo`
 * if resolution succeeds; otherwise, it returns `null` (e.g., when unresolved or ambiguous).
 *
 * [KtNameReferenceExpression] might be resolved not only to callables but also to types.
 *
 * #### Example #2
 *
 * ```kotlin
 * class MyClass
 * object MyObject
 *
 * val c = MyClass()
 * //      ^^^^^^^  resolves to the class `MyClass`
 *
 * val o = MyObject
 * //      ^^^^^^^^  resolves to the object `MyObject`
 * ```
 */
@OptIn(KtExperimentalApi::class)
class KtNameReferenceExpression : KtExpressionImplStub<KotlinNameReferenceExpressionStub>, KtSimpleNameExpression, KtResolvableCall {
    constructor(node: ASTNode) : super(node)

    constructor(stub: KotlinNameReferenceExpressionStub) : super(stub, KtStubBasedElementTypes.REFERENCE_EXPRESSION)

    override fun getReferencedName(): String {
        val stub = greenStub
        if (stub != null) {
            return stub.referencedName
        }
        return KtSimpleNameExpressionImpl.getReferencedNameImpl(this)
    }

    override fun getReferencedNameAsName(): Name {
        return KtSimpleNameExpressionImpl.getReferencedNameAsNameImpl(this)
    }

    override fun getReferencedNameElement(): PsiElement {
        return findChildByType(NAME_REFERENCE_EXPRESSIONS) ?: this
    }

    override fun getIdentifier(): PsiElement? {
        return findChildByType(KtTokens.IDENTIFIER)
    }

    override fun getReferencedNameElementType(): IElementType {
        return KtSimpleNameExpressionImpl.getReferencedNameElementTypeImpl(this)
    }

    override fun <R, D> accept(visitor: KtVisitor<R, D>, data: D): R {
        return visitor.visitSimpleNameExpression(this, data)
    }

    val isPlaceholder: Boolean
        get() = getIdentifier()?.text?.equals("_") == true

    companion object {
        private val NAME_REFERENCE_EXPRESSIONS = TokenSet.create(IDENTIFIER, THIS_KEYWORD, SUPER_KEYWORD)
    }
}
